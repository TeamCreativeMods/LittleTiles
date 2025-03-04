package team.creative.littletiles.common.placement.box;

import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.littletiles.client.render.tile.LittleRenderBox;
import team.creative.littletiles.common.action.LittleActionException;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.placement.Placement;
import team.creative.littletiles.common.structure.LittleStructure;
import team.creative.littletiles.common.structure.directional.StructureDirectionalField;
import team.creative.littletiles.common.structure.relative.StructureRelative;

public class LittlePlaceBoxRelative extends LittlePlaceBox {
    
    public StructureDirectionalField relativeType;
    public StructureRelative relative;
    
    public LittlePlaceBoxRelative(LittleBox box, StructureRelative relative, StructureDirectionalField relativeType) {
        super(box.copy());
        this.relative = relative;
        this.relativeType = relativeType;
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public LittleRenderBox getRenderBox(LittleGrid grid) {
        LittleRenderBox cube = super.getRenderBox(grid);
        cube.color = relativeType.annotation.color();
        return cube;
    }
    
    @Override
    public void place(Placement placement, LittleGrid grid, BlockPos pos, LittleStructure structure) throws LittleActionException {
        relative.setBox(BlockPos.ZERO, box.copy(), grid);
        relative.add(pos.subtract(structure.getPos()));
        relativeType.set(structure, relative);
    }
    
}
