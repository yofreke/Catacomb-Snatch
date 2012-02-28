package com.mojang.mojam.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import com.mojang.mojam.Keys;
import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.MouseButtons;
import com.mojang.mojam.entity.Player;
import com.mojang.mojam.network.Packet;


public class PlayerUpdatePacket extends Packet {
	
	private MojamComponent component;
	
	public PlayerUpdatePacket(){
		component = MojamComponent.instance;
	}
	
	@Override
	public void read(DataInputStream dis) throws IOException {
		int size = dis.readShort();
		for (int i = 0; i < size; i++) {
			short id = dis.readShort();
			boolean flag = dis.readBoolean();
			Player p = component.getPlayer(id);
			System.out.println("PlayerUpdatePacket ("+(component.isServer?"server":"client")+")... "+id+" "+flag);
			if(p == null){
				p = new Player(new Keys(), new MouseButtons(), 0,0,0, id);
				component.registerPlayer(p);
				if(component.level != null) component.level.addEntity(p);
			}
			p.isReady = flag;
		}
	}

	@Override
	public void write(DataOutputStream dos) throws IOException {
		if(component.isServer){
			List<Player> players = component.level.players;
			dos.writeShort(players.size());
			for (int i = 0; i < players.size(); i++) {
				Player player = players.get(i);
				dos.writeShort(player.id);
				dos.writeBoolean(player.isReady);
			}
		} else {
			dos.writeShort(1);
			Player player = component.player;
			dos.writeShort(player.id);
			dos.writeBoolean(player.isReady);
		}
	}

}
