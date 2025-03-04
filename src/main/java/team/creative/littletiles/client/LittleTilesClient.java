package team.creative.littletiles.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent.RegisterAdditional;
import net.minecraftforge.client.event.ModelEvent.RegisterGeometryLoaders;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import team.creative.creativecore.client.CreativeCoreClient;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.client.render.model.CreativeBlockModel;
import team.creative.creativecore.client.render.model.CreativeItemBoxModel;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.littletiles.LittleTiles;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.client.action.LittleActionHandlerClient;
import team.creative.littletiles.client.level.LevelHandlersClient;
import team.creative.littletiles.client.level.LittleAnimationHandlerClient;
import team.creative.littletiles.client.render.block.BETilesRenderer;
import team.creative.littletiles.client.render.entity.RenderSizedTNTPrimed;
import team.creative.littletiles.client.render.item.ItemRenderCache;
import team.creative.littletiles.client.render.item.LittleModelItemBackground;
import team.creative.littletiles.client.render.item.LittleModelItemPreview;
import team.creative.littletiles.client.render.item.LittleModelItemTilesBig;
import team.creative.littletiles.client.render.level.LittleChunkDispatcher;
import team.creative.littletiles.client.render.level.LittleClientEventHandler;
import team.creative.littletiles.client.render.overlay.LittleTilesProfilerOverlay;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.overlay.TooltipOverlay;
import team.creative.littletiles.common.block.little.tile.group.LittleGroup;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.ingredient.BlockIngredientEntry;
import team.creative.littletiles.common.ingredient.ColorIngredient;
import team.creative.littletiles.common.item.ItemBlockIngredient;
import team.creative.littletiles.common.item.ItemColorIngredient;
import team.creative.littletiles.common.item.ItemLittleChisel;
import team.creative.littletiles.common.item.ItemLittleGlove;
import team.creative.littletiles.common.item.ItemLittleGlove.GloveMode;
import team.creative.littletiles.common.item.ItemLittlePaintBrush;
import team.creative.littletiles.common.item.ItemPremadeStructure;
import team.creative.littletiles.common.structure.registry.premade.LittlePremadeRegistry;
import team.creative.littletiles.common.structure.registry.premade.LittlePremadeType;

@OnlyIn(Dist.CLIENT)
public class LittleTilesClient {
    
    public static final Minecraft mc = Minecraft.getInstance();
    
    public static final LevelHandlersClient LEVEL_HANDLERS = new LevelHandlersClient();
    public static LittleActionHandlerClient ACTION_HANDLER;
    public static LittleAnimationHandlerClient ANIMATION_HANDLER;
    public static PreviewRenderer PREVIEW_RENDERER;
    public static ItemRenderCache ITEM_RENDER_CACHE;
    
    public static KeyMapping flip;
    public static KeyMapping mark;
    public static KeyMapping configure;
    public static KeyMapping configureAdvanced;
    public static KeyMapping up;
    public static KeyMapping down;
    public static KeyMapping right;
    public static KeyMapping left;
    
    public static KeyMapping undo;
    public static KeyMapping redo;
    
    public static BETilesRenderer blockEntityRenderer;
    
    public static void displayActionMessage(List<Component> message) {
        // TODO Readd action message overlay
    }
    
    public static void load(IEventBus bus) {
        bus.addListener(LittleTilesClient::setup);
        MinecraftForge.EVENT_BUS.addListener(LittleTilesClient::commands);
        bus.addListener(LittleTilesClient::initColors);
        bus.addListener(LittleTilesClient::registerKeys);
        bus.addListener(LittleTilesClient::modelEvent);
        bus.addListener(LittleTilesClient::modelLoader);
    }
    
    private static void registerKeys(RegisterKeyMappingsEvent event) {
        up = new KeyMapping("key.rotateup", GLFW.GLFW_KEY_UP, "key.categories.littletiles");
        down = new KeyMapping("key.rotatedown", GLFW.GLFW_KEY_DOWN, "key.categories.littletiles");
        right = new KeyMapping("key.rotateright", GLFW.GLFW_KEY_RIGHT, "key.categories.littletiles");
        left = new KeyMapping("key.rotateleft", GLFW.GLFW_KEY_LEFT, "key.categories.littletiles");
        
        flip = new KeyMapping("key.little.flip", GLFW.GLFW_KEY_G, "key.categories.littletiles");
        mark = new KeyMapping("key.little.mark", GLFW.GLFW_KEY_M, "key.categories.littletiles");
        mark = new KeyMapping("key.little.mark", GLFW.GLFW_KEY_M, "key.categories.littletiles");
        configure = new KeyMapping("key.little.config.item", KeyConflictContext.UNIVERSAL, KeyModifier.NONE, InputConstants.Type.KEYSYM, InputConstants.KEY_C, "key.categories.littletiles");
        configureAdvanced = new KeyMapping("key.little.config", KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL, InputConstants.Type.KEYSYM, InputConstants.KEY_C, "key.categories.littletiles");
        
        undo = new KeyMapping("key.little.undo", KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL, InputConstants.Type.KEYSYM, InputConstants.KEY_Z, "key.categories.littletiles");
        redo = new KeyMapping("key.little.redo", KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL, InputConstants.Type.KEYSYM, InputConstants.KEY_Y, "key.categories.littletiles");
        
        event.register(up);
        event.register(down);
        event.register(right);
        event.register(left);
        event.register(flip);
        event.register(mark);
        event.register(configure);
        
        event.register(undo);
        event.register(redo);
    }
    
    private static void setup(final FMLClientSetupEvent event) {
        mc.getItemColors().register((stack, layer) -> {
            if (layer == 0)
                return ColorUtils.WHITE;
            return ItemLittlePaintBrush.getColor(stack);
        }, LittleTilesRegistry.PAINT_BRUSH.get());
        
        // MinecraftForge.EVENT_BUS.register(overlay = new OverlayRenderer());
        // overlay.add(new OverlayControl(new GuiAxisIndicatorControl("axis"), OverlayPositionType.CENTER).setShouldRender(() -> PreviewRenderer.marked != null));
        MinecraftForge.EVENT_BUS.register(new PreviewRenderer());
        MinecraftForge.EVENT_BUS.register(new LittleClientEventHandler());
        
        LEVEL_HANDLERS.register(LittleActionHandlerClient::new, x -> ACTION_HANDLER = x);
        //LEVEL_HANDLERS.register(LittleAnimationHandlerClient::new, x -> ANIMATION_HANDLER = x);
        LEVEL_HANDLERS.register(PREVIEW_RENDERER = new PreviewRenderer());
        LEVEL_HANDLERS.register(ITEM_RENDER_CACHE = new ItemRenderCache());
        
        // Init overlays
        MinecraftForge.EVENT_BUS.register(LittleTilesProfilerOverlay.class);
        MinecraftForge.EVENT_BUS.register(TooltipOverlay.class);
        
        ReloadableResourceManager reloadableResourceManager = (ReloadableResourceManager) mc.getResourceManager();
        reloadableResourceManager.registerReloadListener(new PreparableReloadListener() {
            
            @Override
            public CompletableFuture<Void> reload(PreparationBarrier p_10638_, ResourceManager p_10639_, ProfilerFiller p_10640_, ProfilerFiller p_10641_, Executor p_10642_, Executor p_10643_) {
                return CompletableFuture.runAsync(() -> LittleChunkDispatcher.currentRenderState++, p_10643_);
            }
        });
        
        CreativeCoreClient.registerClientConfig(LittleTiles.MODID);
        
        EntityRenderers.register(LittleTilesRegistry.SIZED_TNT_TYPE.get(), RenderSizedTNTPrimed::new);
        
        blockEntityRenderer = new BETilesRenderer();
        BlockEntityRenderers.register(LittleTilesRegistry.BE_TILES_TYPE_RENDERED.get(), x -> blockEntityRenderer);
        
        ResourceLocation filled = new ResourceLocation(LittleTiles.MODID, "filled");
        ClampedItemPropertyFunction function = (stack, level, entity, x) -> ((ItemColorIngredient) stack.getItem()).getColor(stack) / (float) ColorIngredient.BOTTLE_SIZE;
        ItemProperties.register(LittleTilesRegistry.BLACK_COLOR.get(), filled, function);
        ItemProperties.register(LittleTilesRegistry.CYAN_COLOR.get(), filled, function);
        ItemProperties.register(LittleTilesRegistry.MAGENTA_COLOR.get(), filled, function);
        ItemProperties.register(LittleTilesRegistry.YELLOW_COLOR.get(), filled, function);
    }
    
    public static void modelLoader(RegisterAdditional event) {
        event.register(new ModelResourceLocation(LittleTiles.MODID, "glove_background", "inventory"));
        event.register(new ModelResourceLocation(LittleTiles.MODID, "chisel_background", "inventory"));
        event.register(new ModelResourceLocation(LittleTiles.MODID, "blueprint_background", "inventory"));
    }
    
    public static void modelEvent(RegisterGeometryLoaders event) {
        CreativeCoreClient.registerBlockModel(new ResourceLocation(LittleTiles.MODID, "empty"), new CreativeBlockModel() {
            
            @Override
            public List<? extends RenderBox> getBoxes(BlockState state, ModelData data, RandomSource source) {
                return Collections.EMPTY_LIST;
            }
            
            @Override
            public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
                return modelData;
            }
        });
        
        CreativeCoreClient.registerItemModel(new ResourceLocation(LittleTiles.MODID, "tiles"), new LittleModelItemTilesBig());
        CreativeCoreClient.registerItemModel(new ResourceLocation(LittleTiles.MODID, "premade"), new LittleModelItemTilesBig() {
            @Override
            public List<? extends RenderBox> getBoxes(ItemStack stack, RenderType layer) {
                if (!stack.getOrCreateTag().contains("structure"))
                    return Collections.EMPTY_LIST;
                
                LittlePremadeType premade = LittlePremadeRegistry.get(stack.getOrCreateTagElement("structure").getString("id"));
                if (premade == null)
                    return Collections.EMPTY_LIST;
                LittleGroup previews = ((ItemPremadeStructure) stack.getItem()).getTiles(stack);
                if (previews == null)
                    return Collections.EMPTY_LIST;
                List<RenderBox> cubes = premade.getItemPreview(previews, layer == Sheets.translucentCullBlockSheet());
                if (cubes == null) {
                    cubes = previews.getRenderingBoxes(layer == Sheets.translucentCullBlockSheet());
                    LittleGroup.shrinkCubesToOneBlock(cubes);
                }
                
                return cubes;
            }
        });
        CreativeCoreClient
                .registerItemModel(new ResourceLocation(LittleTiles.MODID, "glove"), new LittleModelItemPreview(new ModelResourceLocation(LittleTiles.MODID, "glove_background", "inventory"), null) {
                    
                    @Override
                    public boolean shouldRenderFake(ItemStack stack) {
                        return true;
                    }
                    
                    @Override
                    protected ItemStack getFakeStack(ItemStack current) {
                        GloveMode mode = ItemLittleGlove.getMode(current);
                        if (mode.renderBlockSeparately(current)) {
                            if (stack == null)
                                stack = new ItemStack(LittleTilesRegistry.ITEM_TILES.get());
                            stack.setTag(current.getTag());
                            return stack;
                        }
                        return new ItemStack(mode.getSeparateRenderingPreview(current).getState().getBlock());
                    }
                });
        CreativeCoreClient
                .registerItemModel(new ResourceLocation(LittleTiles.MODID, "chisel"), new LittleModelItemPreview(new ModelResourceLocation(LittleTiles.MODID, "chisel_background", "inventory"), stack -> ItemLittleChisel
                        .getElement(stack)));
        CreativeCoreClient
                .registerItemModel(new ResourceLocation(LittleTiles.MODID, "blueprint"), new LittleModelItemBackground(new ModelResourceLocation(LittleTiles.MODID, "blueprint_background", "inventory"), () -> new ItemStack(LittleTilesRegistry.ITEM_TILES
                        .get())));
        
        CreativeCoreClient
                .registerItemModel(new ResourceLocation(LittleTiles.MODID, "blockingredient"), new CreativeItemBoxModel(new ModelResourceLocation("miencraft", "stone", "inventory")) {
                    
                    @Override
                    public List<? extends RenderBox> getBoxes(ItemStack stack, RenderType layer) {
                        List<RenderBox> cubes = new ArrayList<>();
                        BlockIngredientEntry ingredient = ItemBlockIngredient.loadIngredient(stack);
                        if (ingredient == null)
                            return null;
                        
                        double volume = Math.min(1, ingredient.value);
                        LittleGrid context = LittleGrid.defaultGrid();
                        long pixels = (long) (volume * context.count3d);
                        if (pixels < context.count * context.count)
                            cubes.add(new RenderBox(0.4F, 0.4F, 0.4F, 0.6F, 0.6F, 0.6F, ingredient.block.getState()));
                        else {
                            long remainingPixels = pixels;
                            long planes = pixels / context.count2d;
                            remainingPixels -= planes * context.count2d;
                            long rows = remainingPixels / context.count;
                            remainingPixels -= rows * context.count;
                            
                            float height = (float) (planes * context.pixelLength);
                            
                            if (planes > 0)
                                cubes.add(new RenderBox(0.0F, 0.0F, 0.0F, 1.0F, height, 1.0F, ingredient.block.getState()));
                            
                            float width = (float) (rows * context.pixelLength);
                            
                            if (rows > 0)
                                cubes.add(new RenderBox(0.0F, height, 0.0F, 1.0F, height + (float) context.pixelLength, width, ingredient.block.getState()));
                            
                            if (remainingPixels > 0)
                                cubes.add(new RenderBox(0.0F, height, width, 1.0F, height + (float) context.pixelLength, width + (float) context.pixelLength, ingredient.block
                                        .getState()));
                        }
                        return cubes;
                    }
                });
    }
    
    public static void initColors(RegisterColorHandlersEvent.Item event) {
        CreativeCoreClient.registerItemColor(event.getItemColors(), LittleTilesRegistry.PREMADE.get());
        CreativeCoreClient.registerItemColor(event.getItemColors(), LittleTilesRegistry.ITEM_TILES.get());
    }
    
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("lt-debug").executes(x -> {
            if (LittleTilesProfilerOverlay.isActive())
                LittleTilesProfilerOverlay.stop();
            else
                LittleTilesProfilerOverlay.start();
            return Command.SINGLE_SUCCESS;
        }));
    }
    
}
