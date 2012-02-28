package com.mojang.mojam.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.mojang.mojam.Keys;
import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.MouseButtons;
import com.mojang.mojam.entity.Player;
import com.mojang.mojam.network.Packet;

public class SetPlayerPacket extends Packet {

	private MojamComponent component;
	private Player toSend;
	
	public SetPlayerPacket(){
		component = MojamComponent.instance;
	}
	public SetPlayerPacket(Player player){
		this.toSend = player;
	}
	
	@Override
	public void read(DataInputStream dis) throws IOException {
		double x = dis.readDouble();
		double y = dis.readDouble();
		int team = dis.readShort();
		short id = dis.readShort();
		System.out.println("SetPlayerPacket ... "+id+" "+team+" "+x+","+y);
		
		if(component.isServer) return;
		if(component.player == null){
			System.out.println("  new player instance.");
			component.player = new Player(component.keys, component.mouseButtons, (int)(x+.5), (int)(y+.5), team, id);
			component.registerPlayer(component.player);
			if(component.level != null) component.level.addEntity(component.player);
		} else {
			System.out.println("  update current instance.");
			component.player.setTeam(team);
			component.player.setId(id);
		}
		component.player.setPos(x, y);
		component.setLocalId(id);
	}

	@Override
	public void write(DataOutputStream dos) throws IOException {
		dos.writeDouble(toSend.pos.x);
		dos.writeDouble(toSend.pos.y);
		dos.writeShort(toSend.getTeam());
		dos.writeShort(toSend.id);
	}

}
