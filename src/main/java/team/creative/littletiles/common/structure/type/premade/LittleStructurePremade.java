package team.creative.littletiles.common.structure.type.premade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.MixinEnvironment.Side;

import com.creativemd.creativecore.common.utils.mc.NBTUtils;
import com.creativemd.littletiles.common.tile.preview.LittlePreview;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;
import com.google.common.base.Charsets;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.relauncher.SideOnly;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.common.gui.GuiParent;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.common.animation.AnimationGuiHandler;
import team.creative.littletiles.common.block.little.tile.group.LittleGroup;
import team.creative.littletiles.common.block.little.tile.parent.IStructureParentCollection;
import team.creative.littletiles.common.item.ItemPremadeStructure;
import team.creative.littletiles.common.structure.LittleStructure;
import team.creative.littletiles.common.structure.LittleStructureAttribute;
import team.creative.littletiles.common.structure.LittleStructureType;
import team.creative.littletiles.common.structure.exception.CorruptedConnectionException;
import team.creative.littletiles.common.structure.exception.NotYetConnectedException;
import team.creative.littletiles.common.structure.registry.LittleStructureGuiParser;
import team.creative.littletiles.common.structure.registry.LittleStructureGuiParser.LittleStructureGuiParserNotFoundHandler;
import team.creative.littletiles.common.structure.registry.LittleStructureRegistry;
import team.creative.littletiles.common.structure.signal.logic.SignalMode;
import team.creative.littletiles.common.structure.type.premade.LittleParticleEmitter.LittleStructureTypeParticleEmitter;
import team.creative.littletiles.common.structure.type.premade.signal.LittleSignalCable;
import team.creative.littletiles.common.structure.type.premade.signal.LittleSignalCable.LittleStructureTypeCable;
import team.creative.littletiles.common.structure.type.premade.signal.LittleSignalDisplay;
import team.creative.littletiles.common.structure.type.premade.signal.LittleSignalInput;
import team.creative.littletiles.common.structure.type.premade.signal.LittleSignalInput.LittleStructureTypeInput;
import team.creative.littletiles.common.structure.type.premade.signal.LittleSignalOutput;
import team.creative.littletiles.common.structure.type.premade.signal.LittleSignalOutput.LittleStructureTypeOutput;

public abstract class LittleStructurePremade extends LittleStructure {
    
    public LittleStructurePremade(LittleStructureType type, IStructureParentCollection mainBlock) {
        super(type, mainBlock);
    }
    
    private static LinkedHashMap<String, LittleStructurePremadeEntry> structurePreviews = new LinkedHashMap<>();
    private static List<LittleStructureTypePremade> premadeStructures = new ArrayList<>();
    
    private static JsonParser parser = new JsonParser();
    
    public static void reloadPremadeStructures() {
        
        structurePreviews.clear();
        ItemPremadeStructure.clearCache();
        
        for (LittleStructureTypePremade type : premadeStructures) {
            try {
                ItemStack stack = type.createItemStackEmpty();
                NBTTagCompound structureNBT = new NBTTagCompound();
                structureNBT.setString("id", type.id);
                NBTTagCompound nbt = JsonToNBT.getTagFromJson(IOUtils
                        .toString(LittleStructurePremade.class.getClassLoader().getResourceAsStream("assets/" + type.modid + "/premade/" + type.id + ".struct"), Charsets.UTF_8));
                NBTTagCompound originalNBT = nbt.hasKey("structure") ? nbt.getCompoundTag("structure") : null;
                nbt.setTag("structure", structureNBT);
                if (originalNBT != null)
                    NBTUtils.mergeNotOverwrite(structureNBT, originalNBT);
                stack.setTagCompound(nbt);
                LittlePreviews previews = LittlePreview.getPreview(stack);
                
                NBTTagCompound stackNBT = new NBTTagCompound();
                stackNBT.setTag("structure", structureNBT);
                stack.setTagCompound(stackNBT);
                
                structurePreviews.put(type.id, new LittleStructurePremadeEntry(previews, stack));
                System.out.println("Loaded " + type.id + " model");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Could not load '" + type.id + "'! Structure will not be registered");
            }
        }
    }
    
    public static LittleStructureTypePremade registerPremadeStructureType(String id, String modid, Class<? extends LittleStructurePremade> classStructure) {
        return registerPremadeStructureType(id, modid, classStructure, LittleStructureAttribute.NONE);
    }
    
    public static LittleStructureTypePremade registerPremadeStructureType(String id, String modid, Class<? extends LittleStructurePremade> classStructure, int attribute) {
        LittleStructureTypePremade type = (LittleStructureTypePremade) LittleStructureRegistry
                .registerStructureType(new LittleStructureTypePremade(id, "premade", classStructure, attribute, modid), null);
        premadeStructures.add(type);
        return type;
    }
    
    public static LittleStructureTypePremade registerPremadeStructureType(LittleStructureTypePremade type) {
        premadeStructures.add((LittleStructureTypePremade) LittleStructureRegistry.registerStructureType(type, null));
        return type;
    }
    
    public static LittlePreviews getPreviews(String id) {
        LittleStructurePremadeEntry type = getStructurePremadeEntry(id);
        if (type != null)
            return type.previews;
        return null;
    }
    
    public static LittleStructurePremadeEntry getStructurePremadeEntry(String id) {
        return structurePreviews.get(id);
    }
    
    public static Collection<LittleStructurePremadeEntry> getPremadeStructures() {
        return structurePreviews.values();
    }
    
    public static List<LittleStructureTypePremade> getPremadeStructureTypes() {
        return premadeStructures;
    }
    
    public static Set<String> getPremadeStructureIds() {
        return structurePreviews.keySet();
    }
    
    public static LittleStructureTypePremade getType(String id) {
        LittleStructureType type = LittleStructureRegistry.getStructureType(id);
        if (type instanceof LittleStructureTypePremade)
            return (LittleStructureTypePremade) type;
        return null;
    }
    
    public static ItemStack tryGetPremadeStack(String id) {
        LittleStructurePremadeEntry entry = structurePreviews.get(id);
        if (entry != null)
            return entry.stack.copy();
        return ItemStack.EMPTY;
    }
    
    public static ItemStack getPremadeStack(String id) {
        return structurePreviews.get(id).stack.copy();
    }
    
    @Override
    public ItemStack getStructureDrop() throws CorruptedConnectionException, NotYetConnectedException {
        ItemStack stack = getPremadeStack(type.id).copy();
        
        load();
        BlockPos pos = getMinPos(getPos().mutable());
        
        CompoundTag structureNBT = new CompoundTag();
        this.writeToNBTPreview(structureNBT, pos);
        
        if (!stack.hasTag())
            stack.setTag(new CompoundTag());
        stack.getTag().put("structure", structureNBT);
        
        if (name != null) {
            CompoundTag display = new CompoundTag();
            display.putString("Name", name);
            stack.getTag().put("display", display);
        }
        return stack;
    }
    
    public static void initPremadeStructures() {
        registerPremadeStructureType("workbench", LittleTiles.MODID, LittleWorkbench.class);
        registerPremadeStructureType("importer", LittleTiles.MODID, LittleImporter.class);
        registerPremadeStructureType("exporter", LittleTiles.MODID, LittleExporter.class);
        registerPremadeStructureType(new LittleStructureTypeParticleEmitter("particle_emitter", "premade", LittleParticleEmitter.class, LittleStructureAttribute.TICKING, LittleTiles.MODID))
                .addOutput("disabled", 1, SignalMode.TOGGLE, true).setFieldDefault("facing", EnumFacing.UP);
        registerPremadeStructureType("blankomatic", LittleTiles.MODID, LittleBlankOMatic.class);
        
        registerPremadeStructureType(new LittleStructureTypeCable("single_cable1", "premade", LittleSignalCable.class, LittleStructureAttribute.EXTRA_RENDERING, LittleTiles.MODID, 1));
        registerPremadeStructureType(new LittleStructureTypeCable("single_cable4", "premade", LittleSignalCable.class, LittleStructureAttribute.EXTRA_RENDERING, LittleTiles.MODID, 4));
        registerPremadeStructureType(new LittleStructureTypeCable("single_cable16", "premade", LittleSignalCable.class, LittleStructureAttribute.EXTRA_RENDERING, LittleTiles.MODID, 16));
        
        registerPremadeStructureType(new LittleStructureTypeOutput("single_output1", "premade", LittleSignalOutput.class, LittleStructureAttribute.EXTRA_RENDERING, LittleTiles.MODID, 1));
        registerPremadeStructureType(new LittleStructureTypeOutput("single_output4", "premade", LittleSignalOutput.class, LittleStructureAttribute.EXTRA_RENDERING, LittleTiles.MODID, 4));
        registerPremadeStructureType(new LittleStructureTypeOutput("single_output16", "premade", LittleSignalOutput.class, LittleStructureAttribute.EXTRA_RENDERING, LittleTiles.MODID, 16));
        
        registerPremadeStructureType(new LittleStructureTypeInput("single_input1", "premade", LittleSignalInput.class, LittleStructureAttribute.EXTRA_RENDERING, LittleTiles.MODID, 1));
        registerPremadeStructureType(new LittleStructureTypeInput("single_input4", "premade", LittleSignalInput.class, LittleStructureAttribute.EXTRA_RENDERING, LittleTiles.MODID, 4));
        registerPremadeStructureType(new LittleStructureTypeInput("single_input16", "premade", LittleSignalInput.class, LittleStructureAttribute.EXTRA_RENDERING, LittleTiles.MODID, 16));
        
        registerPremadeStructureType("signal_display_16", LittleTiles.MODID, LittleSignalDisplay.class, LittleStructureAttribute.TICK_RENDERING)
                .addOutput("pixels", 16, SignalMode.EQUAL, true);
        
        registerPremadeStructureType("structure_builder", LittleTiles.MODID, LittleStructureBuilder.class);
        
        LittleStructureRegistry.registerGuiParserNotFoundHandler(new LittleStructureGuiParserNotFoundHandler() {
            
            @Override
            @SideOnly(Side.CLIENT)
            public LittleStructureGuiParser create(LittleStructure structure, GuiParent parent, AnimationGuiHandler handler) {
                if (structure instanceof LittleStructurePremade)
                    return new LittleStructureGuiParser(parent, handler) {
                        
                        @Override
                        @SideOnly(Side.CLIENT)
                        protected void createControls(LittlePreviews previews, LittleStructure structure) {
                        
                        }
                        
                        @Override
                        protected LittleStructure parseStructure(LittlePreviews previews) {
                            LittleStructure parsedStructure = structure.type.createStructure(null);
                            parsedStructure.loadFromNBT(previews.structureNBT);
                            return parsedStructure;
                        }
                        
                        @Override
                        protected LittleStructureType getStructureType() {
                            return structure.type;
                        }
                        
                    };
                return null;
            }
        });
    }
    
    public static class LittleStructureTypePremade extends LittleStructureType {
        
        public final String modid;
        public boolean showInCreativeTab = true;
        public boolean snapToGrid = true;
        
        public LittleStructureTypePremade(String id, String category, Class<? extends LittleStructure> structureClass, int attribute, String modid) {
            super(id, category, structureClass, LittleStructureAttribute.PREMADE | attribute);
            this.modid = modid;
        }
        
        public ItemStack createItemStackEmpty() {
            return new ItemStack(LittleTiles.premade);
        }
        
        public ItemStack createItemStack() {
            ItemStack stack = createItemStackEmpty();
            NBTTagCompound structureNBT = new NBTTagCompound();
            structureNBT.setString("id", id);
            NBTTagCompound stackNBT = new NBTTagCompound();
            stackNBT.setTag("structure", structureNBT);
            stack.setTagCompound(stackNBT);
            return stack;
        }
        
        public LittleStructureTypePremade setNotShowCreativeTab() {
            this.showInCreativeTab = false;
            return this;
        }
        
        public LittleStructureTypePremade setNotSnapToGrid() {
            this.snapToGrid = false;
            return this;
        }
        
        @OnlyIn(Dist.CLIENT)
        public List<RenderBox> getRenderingCubes(LittleGroup previews) {
            return null;
        }
        
        @Override
        public boolean canOnlyBePlacedByItemStack() {
            return true;
        }
        
    }
    
    public static class LittleStructurePremadeEntry {
        
        public final LittleGroup previews;
        public final ItemStack stack;
        
        public LittleStructurePremadeEntry(LittleGroup previews, ItemStack stack) {
            this.previews = previews;
            this.stack = stack;
        }
        
        public boolean arePreviewsEqual(LittleGroup previews) {
            return this.previews.isVolumeEqual(previews);
        }
    }
    
}
