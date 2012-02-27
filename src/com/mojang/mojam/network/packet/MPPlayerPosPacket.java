package com.mojang.mojam.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.mojang.mojam.entity.EntityList;
import com.mojang.mojam.entity.Player;
import com.mojang.mojam.network.Packet;

public class MPPlayerPosPacket extends Packet {

	private Player player;
	
	public MPPlayerPosPacket(){}
	public MPPlayerPosPacket(Player player){
		this.player = player;
	}
	
	@Override
	public void read(DataInputStream dis) throws IOException {
		player = (Player) EntityList.idToEntityMap.get(dis.readShort());
		player.setPos(dis.readDouble(), dis.readDouble());
	}

	@Override
	public void write(DataOutputStream dos) throws IOException {
		dos.writeShort(player.id);
		dos.writeDouble(player.pos.x);
		dos.writeDouble(player.pos.y);
	}
}
