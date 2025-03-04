package team.creative.littletiles.common.api.block;

import com.mojang.math.Vector3d;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import team.creative.creativecore.common.util.math.base.Axis;
import team.creative.creativecore.common.util.math.transformation.Rotation;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.littletiles.client.render.tile.LittleRenderBox;
import team.creative.littletiles.common.block.little.element.LittleElement;
import team.creative.littletiles.common.block.little.registry.LittleMCBlock;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.little.tile.parent.IParentCollection;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.vec.LittleVec;

public interface ILittleMCBlock extends LittleBlock {
    
    static final RandomSource RANDOM = RandomSource.create();
    
    public static boolean isTranslucent(Block block) {
        return !block.defaultBlockState().getMaterial().isSolid() || !block.defaultBlockState().getMaterial().isSolid() || block.defaultBlockState().canOcclude();
    }
    
    public Block asBlock();
    
    @Override
    public default boolean isTranslucent() {
        return isTranslucent(asBlock());
    }
    
    @Override
    public default boolean is(ItemStack stack) {
        return Block.byItem(stack.getItem()) == asBlock();
    }
    
    @Override
    public default boolean is(TagKey<Block> tag) {
        return asBlock().builtInRegistryHolder().is(tag);
    }
    
    @Override
    public default ItemStack getStack() {
        return new ItemStack(asBlock());
    }
    
    @Override
    public default boolean canBeRenderCombined(LittleTile one, LittleTile two) {
        return one.getBlock() == two.getBlock() && one.color == two.color;
    }
    
    @Override
    public default boolean noCollision() {
        try {
            return !LittleMCBlock.hasCollisionField.getBoolean(asBlock());
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    @Override
    public default boolean is(Block block) {
        return this.asBlock() == block;
    }
    
    @Override
    public default boolean canBeConvertedToVanilla() {
        return true;
    }
    
    @Override
    public default BlockState mirror(BlockState state, Axis axis, LittleVec doubledCenter) {
        return state;
    }
    
    @Override
    public default BlockState rotate(BlockState state, Rotation rotation, LittleVec doubledCenter) {
        return state;
    }
    
    @Override
    public default SoundType getSoundType() {
        return asBlock().getSoundType(getState());
    }
    
    @Override
    public default float getExplosionResistance(LittleTile tile) {
        return asBlock().getExplosionResistance();
    }
    
    @Override
    public default void exploded(IParentCollection parent, LittleTile tile, Explosion explosion) {}
    
    @Override
    public default void randomDisplayTick(IParentCollection parent, LittleTile tile, RandomSource rand) {}
    
    @Override
    public default int getLightValue() {
        return getState().getLightEmission();
    }
    
    @Override
    public default BlockState getState() {
        return asBlock().defaultBlockState();
    }
    
    @Override
    public default String blockName() {
        return asBlock().builtInRegistryHolder().key().location().toString();
    }
    
    @Override
    public default boolean canInteract() {
        return false;
    }
    
    @Override
    public default InteractionResult use(IParentCollection parent, LittleTile tile, LittleBox box, Player player, BlockHitResult result) {
        return InteractionResult.PASS;
    }
    
    @Override
    public default float getEnchantPowerBonus(IParentCollection parent, LittleTile tile) {
        float bonus = asBlock().getEnchantPowerBonus(getState(), parent.getLevel(), parent.getPos());
        if (bonus > 0)
            return (float) (bonus * tile.getPercentVolume(parent.getGrid()));
        return 0;
    }
    
    @Override
    public default float getFriction(IParentCollection parent, LittleTile tile, Entity entity) {
        return asBlock().getFriction(getState(), parent.getLevel(), parent.getPos(), entity);
    }
    
    @Override
    public default boolean isMaterial(Material material) {
        return getState().getMaterial() == material;
    }
    
    @Override
    public default boolean isLiquid() {
        return getState().getMaterial().isLiquid();
    }
    
    @Override
    public default LittleRenderBox getRenderBox(LittleGrid grid, RenderType layer, LittleBox box, LittleElement element) {
        return new LittleRenderBox(grid, box, element);
    }
    
    @Override
    public default boolean checkEntityCollision() {
        return false;
    }
    
    @Override
    public default void entityCollided(IParentCollection parent, LittleTile tile, Entity entity) {}
    
    @Override
    public default Vector3d getFogColor(IParentCollection parent, LittleTile tile, Entity entity, Vector3d originalColor, float partialTicks) {
        return originalColor;
    }
    
    @Override
    public default Vec3d modifyAcceleration(IParentCollection parent, LittleTile tile, Entity entity, Vec3d motion) {
        return motion;
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public default boolean canRenderInLayer(LittleTile tile, RenderType layer) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(getState()).getRenderTypes(getState(), RANDOM, ModelData.EMPTY).contains(layer);
    }
    
}
