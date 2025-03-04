package team.creative.littletiles.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
    
    @Accessor
    public ViewArea getViewArea();
    
}
