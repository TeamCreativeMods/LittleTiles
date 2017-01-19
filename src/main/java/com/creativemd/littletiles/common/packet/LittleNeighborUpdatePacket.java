package com.creativemd.littletiles.common.packet;

import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.littletiles.common.blocks.BlockTile;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class LittleNeighborUpdatePacket extends CreativeCorePacket {
	
	public BlockPos pos;
	
	public LittleNeighborUpdatePacket(BlockPos pos) {
		this.pos = pos;
	}
	
	public LittleNeighborUpdatePacket() {
		
	}
	
	@Override
	public void writeBytes(ByteBuf buf) {
		writePos(buf, pos);
	}

	@Override
	public void readBytes(ByteBuf buf) {
		pos = readPos(buf);
	}

	@Override
	public void executeClient(EntityPlayer player) {
		IBlockState state = player.getEntityWorld().getBlockState(pos);
		if(state.getBlock() instanceof BlockTile)
			state.getBlock().neighborChanged(state, player.getEntityWorld(), pos, state.getBlock());
	}

	@Override
	public void executeServer(EntityPlayer player) {
		
	}

}
