package team.creative.littletiles.common.structure.registry;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.MixinEnvironment.Side;

import com.creativemd.littletiles.client.gui.controls.IAnimationControl;
import com.creativemd.littletiles.client.gui.dialogs.SubGuiSignalEvents.GuiSignalEventsButton;
import com.creativemd.littletiles.common.entity.AnimationPreview;
import com.creativemd.littletiles.common.tile.preview.LittlePreviews;

import net.minecraftforge.fml.relauncher.SideOnly;
import team.creative.creativecore.common.gui.GuiParent;
import team.creative.littletiles.common.animation.AnimationGuiHandler;
import team.creative.littletiles.common.block.little.tile.parent.StructureParentCollection;
import team.creative.littletiles.common.structure.LittleStructure;
import team.creative.littletiles.common.structure.LittleStructureType;

public abstract class LittleStructureGuiParser implements IAnimationControl {
    
    public final GuiParent parent;
    public final AnimationGuiHandler handler;
    
    public LittleStructureGuiParser(GuiParent parent, AnimationGuiHandler handler) {
        this.parent = parent;
        this.handler = handler;
    }
    
    @SideOnly(Side.CLIENT)
    public void create(LittlePreviews previews, @Nullable LittleStructure structure) {
        createControls(previews, structure);
        parent.controls.add(new GuiSignalEventsButton("signal", 0, 122, previews, structure, getStructureType()));
        
    }
    
    @SideOnly(Side.CLIENT)
    public LittleStructure parse(LittlePreviews previews) {
        LittleStructure structure = parseStructure(previews);
        GuiSignalEventsButton button = (GuiSignalEventsButton) parent.get("signal");
        button.setEventsInStructure(structure);
        return structure;
    }
    
    @SideOnly(Side.CLIENT)
    protected abstract void createControls(LittlePreviews previews, @Nullable LittleStructure structure);
    
    @SideOnly(Side.CLIENT)
    protected abstract LittleStructure parseStructure(LittlePreviews previews);
    
    @SideOnly(Side.CLIENT)
    protected abstract LittleStructureType getStructureType();
    
    public <T extends LittleStructure> T createStructure(Class<T> structureClass, StructureParentCollection parent) {
        LittleStructureType type = LittleStructureRegistry.getStructureType(structureClass);
        if (type == null)
            throw new RuntimeException("Could find structure for " + structureClass);
        return (T) type.createStructure(parent);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void onLoaded(AnimationPreview animationPreview) {
        
    }
    
    public static abstract class LittleStructureGuiParserNotFoundHandler {
        
        @SideOnly(Side.CLIENT)
        public abstract LittleStructureGuiParser create(LittleStructure structure, GuiParent parent, AnimationGuiHandler handler);
        
    }
    
}
