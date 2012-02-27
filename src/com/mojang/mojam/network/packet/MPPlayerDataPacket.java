package com.mojang.mojam.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.mojang.mojam.network.Packet;

public class MPPlayerDataPacket extends Packet {

	private int playerId;
	
	public MPPlayerDataPacket(){
	}
	public MPPlayerDataPacket(int playerId){
		this.playerId = playerId;
	}
	
	@Override
	public void read(DataInputStream dis) throws IOException {
		playerId = dis.readShort();
	}

	@Override
	public void write(DataOutputStream dos) throws IOException {
		dos.writeShort(playerId);
	}

}
