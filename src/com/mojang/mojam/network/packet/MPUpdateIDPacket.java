package com.mojang.mojam.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.entity.Entity;
import com.mojang.mojam.entity.EntityList;
import com.mojang.mojam.network.Packet;

public class MPUpdateIDPacket extends Packet {
	
	private static ArrayList<Short> oldIds = new ArrayList<Short>();
	private static ArrayList<Short> newIds = new ArrayList<Short>();
	
	public MPUpdateIDPacket() {
	}
	
	public static void addIdChange(short oldId, short newId){
		oldIds.add(oldId);
		newIds.add(newId);
	}
	
	public static void tick(MojamComponent component){
		if(oldIds.size() > 0){
			component.packetLink.sendPacket(new MPUpdateIDPacket());
		}
	}
	
	@Override
	public void read(DataInputStream dis) throws IOException {
		int size = dis.readShort();
		for (int i = 0; i < size; i++) {
			short o = dis.readShort();
			short n = dis.readShort();
			Entity e = EntityList.idToEntityMap.get(o);
			//System.out.println(o+" >> "+n);
			if(e == null){
				//System.out.println("  null");
				continue;
			}
			e.setId(n);
		}
	}

	@Override
	public void write(DataOutputStream dos) throws IOException {
		dos.writeShort(oldIds.size());
		for (int i = 0; i < oldIds.size(); i++) {
			dos.writeShort(oldIds.get(i));
			dos.writeShort(newIds.get(i));
		}
		oldIds.clear();
		newIds.clear();
	}

}
