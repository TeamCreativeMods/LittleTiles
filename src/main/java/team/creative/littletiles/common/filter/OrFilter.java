package team.creative.littletiles.common.filter;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.Constants;
import team.creative.littletiles.common.tile.LittleTile;
import team.creative.littletiles.common.tile.parent.IParentTileList;

public class OrFilter extends TileFilter {
    
    public TileFilter[] selectors;
    
    public OrFilter(TileFilter... selectors) {
        this.selectors = selectors;
    }
    
    public OrFilter() {
        
    }
    
    @Override
    protected void saveNBT(CompoundNBT nbt) {
        ListNBT list = new ListNBT();
        for (int i = 0; i < selectors.length; i++)
            list.add(selectors[i].writeNBT(new CompoundNBT()));
        nbt.put("list", list);
    }
    
    @Override
    protected void loadNBT(CompoundNBT nbt) {
        ListNBT list = nbt.getList("list", Constants.NBT.TAG_COMPOUND);
        selectors = new TileFilter[list.size()];
        for (int i = 0; i < selectors.length; i++)
            selectors[i] = TileFilter.load(list.getCompound(i));
    }
    
    @Override
    public boolean is(IParentTileList parent, LittleTile tile) {
        for (int i = 0; i < selectors.length; i++)
            if (selectors[i].is(parent, tile))
                return true;
        return false;
    }
    
}
