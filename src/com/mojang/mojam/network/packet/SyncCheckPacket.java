package com.mojang.mojam.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.mojang.mojam.network.Packet;
import com.mojang.mojam.network.TurnSynchronizer;

public class SyncCheckPacket extends Packet {

	public static long lastTest;
	public long rn;
	
	public SyncCheckPacket(){
	}
	public SyncCheckPacket(long rn){
		this.rn = rn;
	}
	
	@Override
	public void read(DataInputStream dis) throws IOException {
		System.out.println(System.currentTimeMillis()+": sync read");
		rn = dis.readLong();
	}

	@Override
	public void write(DataOutputStream dos) throws IOException {
		System.out.println(System.currentTimeMillis()+": sync write");
		dos.writeLong(rn);
	}

}
