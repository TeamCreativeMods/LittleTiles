package team.creative.littletiles.common.structure.type.premade.signal;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.creativecore.common.util.math.base.Axis;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.box.AlignedBox;
import team.creative.creativecore.common.util.math.box.BoxCorner;
import team.creative.creativecore.common.util.math.box.BoxFace;
import team.creative.creativecore.common.util.math.vec.Vec3f;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.littletiles.common.block.little.tile.parent.IStructureParentCollection;
import team.creative.littletiles.common.structure.LittleStructureType;
import team.creative.littletiles.common.structure.directional.StructureDirectional;
import team.creative.littletiles.common.structure.relative.StructureRelative;
import team.creative.littletiles.common.structure.signal.SignalState;
import team.creative.littletiles.common.structure.signal.output.InternalSignalOutput;
import team.creative.littletiles.common.structure.type.premade.LittleStructurePremade;

public class LittleSignalDisplay extends LittleStructurePremade {
    
    public static final int renderDistance = 64;
    
    @StructureDirectional(color = ColorUtils.CYAN)
    public StructureRelative frame;
    
    @StructureDirectional
    public Facing facing;
    
    @StructureDirectional
    public Vec3f topRight;
    
    private int textureId = -1;
    
    public LittleSignalDisplay(LittleStructureType type, IStructureParentCollection mainBlock) {
        super(type, mainBlock);
    }
    
    @Override
    protected void loadExtra(CompoundTag nbt) {}
    
    @Override
    protected void saveExtra(CompoundTag nbt) {}
    
    @Override
    public void receiveInternalOutputChange(InternalSignalOutput output) {
        if (output.component.is("pixels") && isClient())
            updateTexture();
    }
    
    @OnlyIn(Dist.CLIENT)
    public void updateTexture() {
        if (textureId == -1)
            textureId = GlStateManager._genTexture();
        GlStateManager._bindTexture(textureId);
        int bandwidth = getOutput(0).getBandwidth();
        SignalState state = getOutput(0).getState();
        ByteBuffer buffer = ByteBuffer.allocateDirect(bandwidth * 3);
        for (int i = 0; i < bandwidth; i++) {
            if (state.is(i)) {
                buffer.put((byte) 255);
                buffer.put((byte) 255);
                buffer.put((byte) 255);
            } else {
                buffer.put((byte) 0);
                buffer.put((byte) 0);
                buffer.put((byte) 0);
            }
        }
        buffer.rewind();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA2, 4, 4, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderTick(PoseStack pose, MultiBufferSource buffer, BlockPos pos, float partialTickTime) {
        super.renderTick(pose, buffer, pos, partialTickTime);
        if (textureId == -1)
            updateTexture();
        
        RenderSystem.bindTexture(textureId);
        
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        
        pose.pushPose();
        
        AlignedBox box = frame.getBox().getBox(frame.getGrid());
        BoxFace face = BoxFace.get(facing);
        if (facing.positive)
            box.setMax(facing.axis, box.getMin(facing.axis) + 0.01F);
        else
            box.setMin(facing.axis, box.getMax(facing.axis) - 0.01F);
        Axis uAxis = face.getTexUAxis();
        Axis vAxis = face.getTexVAxis();
        
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder builder = tessellator.getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (BoxCorner corner : face.corners)
            builder.vertex(pose.last().pose(), box.get(corner.x), box.get(corner.y), box.get(corner.z))
                    .uv(corner.isFacingPositive(uAxis) != (topRight.get(uAxis) > 0) ? 1 : 0, corner.isFacingPositive(vAxis) != (topRight.get(vAxis) > 0) ? 1 : 0).endVertex();
        tessellator.end();
        
        pose.popPose();
        
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public double getMaxRenderDistance() {
        return renderDistance;
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public AABB getRenderBoundingBox() {
        return frame.getBox().getBB(frame.getGrid());
    }
    
    @Override
    public void unload() {
        super.unload();
        if (getLevel().isClientSide && textureId != -1)
            GlStateManager._deleteTexture(textureId);
    }
    
}
