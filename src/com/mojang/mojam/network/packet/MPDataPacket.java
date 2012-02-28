package com.mojang.mojam.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import javax.management.RuntimeErrorException;

import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.entity.Entity;
import com.mojang.mojam.entity.EntityList;
import com.mojang.mojam.entity.Player;
import com.mojang.mojam.entity.loot.Loot;
import com.mojang.mojam.entity.mob.Mob;
import com.mojang.mojam.level.tile.Tile;
import com.mojang.mojam.network.Packet;

public class MPDataPacket extends Packet {
	
	public MojamComponent component;
	public short playerId;
	
	public MPDataPacket(){
		this.component = MojamComponent.instance;
	}
	public MPDataPacket(MojamComponent component, short playerId){
		this.component = component;
		this.playerId = playerId;
	}
	
	@Override
	public void read(DataInputStream dis) throws IOException {
		int size = dis.readShort();
	    for(int i = 0; i < size; i++) {
	    	short id = dis.readShort();
	    	short type = dis.readShort();
	    	double xPos = dis.readDouble();
	    	double yPos = dis.readDouble();
	    	double xVel = dis.readDouble();
	    	double yVel = dis.readDouble();
	    	Entity e = EntityList.idToEntityMap.get(id);
			if(e == null){
				// make the entity, because it hasnt been created yet
				e = EntityList.shortToEntity(type, xPos, yPos);
				if(e == null){
					throw(new RuntimeException("BAD ENTITY: "+id+" : "+type));
				}
				if(component.level != null) component.level.addEntity(e);
			} else {
				e.setPos(xPos, yPos);				
			}
			e.setId(id);
			e.xd = xVel;
			e.yd = yVel;
			
			if(e instanceof Mob){
				Mob m = (Mob) e;
				m.setTeam(dis.readShort());
				m.health = dis.readFloat();
				m.isImmortal = dis.readBoolean();
				m.hurtTime = dis.readShort();
			} else if(e instanceof Loot){
				Loot l = (Loot) e;
				l.setValue(dis.readShort());
				l.removed = dis.readBoolean();
			}
			
		}
	    
	    if(System.currentTimeMillis() % 1000 < 30){
	    	System.out.println("Recieve done, "+size+" OF "+component.level.entities.size());
	    }
	}
	
	@Override
	public void write(DataOutputStream dos) throws IOException {
		Player player = component.getPlayer(playerId);
		int xScroll = (int) (player.pos.x - MojamComponent.GAME_WIDTH / 2);
		int yScroll = (int) (player.pos.y - (MojamComponent.GAME_HEIGHT - 24) / 2);
		Set<Entity> visibleEntities = component.level.getEntities(xScroll - Tile.WIDTH, yScroll
				- Tile.HEIGHT, xScroll + MojamComponent.GAME_WIDTH -+Tile.WIDTH, yScroll
				+ MojamComponent.GAME_HEIGHT + Tile.HEIGHT);
		
		ArrayList<Entity> toSend = new ArrayList<Entity>();
		Iterator<Entity> iter = visibleEntities.iterator();
	    while (iter.hasNext()) {
			Entity e = iter.next();
			if(e.type > 0 && EntityList.mpTransfer.contains(e.type)){
				toSend.add(e);
			}
	    }
	    if(System.currentTimeMillis() % 1000 < 30){
	    	//System.out.println(player.name+": "+mobs.size()+"/"+visibleEntities.size()+" OF "+component.level.entities.size());
	    }
		
	    dos.writeShort(toSend.size());
	    for(Entity e : toSend){
			dos.writeShort(e.id);
			dos.writeShort(e.type);
			dos.writeDouble(e.pos.x);
			dos.writeDouble(e.pos.y);
			dos.writeDouble(e.xd);
			dos.writeDouble(e.yd);
			
	    	if(e instanceof Mob){
	    		Mob m = (Mob) e;
				dos.writeShort(m.getTeam());
				dos.writeFloat(m.health);
				dos.writeBoolean(m.isImmortal);
				dos.writeShort(m.hurtTime);
	    	} else if(e instanceof Loot){
	    		Loot l = (Loot) e;
				dos.writeShort(l.getScoreValue());
				dos.writeBoolean(l.removed);
	    	}
	    }
	}
	
}
