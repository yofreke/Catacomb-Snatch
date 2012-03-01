package com.mojang.mojam.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.entity.Bullet;
import com.mojang.mojam.entity.Entity;
import com.mojang.mojam.entity.EntityList;
import com.mojang.mojam.entity.Player;
import com.mojang.mojam.entity.building.SpawnerEntity;
import com.mojang.mojam.entity.loot.Loot;
import com.mojang.mojam.entity.mob.Bat;
import com.mojang.mojam.entity.mob.Mob;
import com.mojang.mojam.level.EntityComparator;
import com.mojang.mojam.level.Level;
import com.mojang.mojam.level.tile.Tile;
import com.mojang.mojam.network.Packet;

public class MPDataPacket extends Packet {

	public MojamComponent component;
	public short playerId;
	private ArrayList<Entity> toSend;

	public MPDataPacket(){
		this.component = MojamComponent.instance;
	}
	public MPDataPacket(MojamComponent component, short playerId){
		this.component = component;
		this.playerId = playerId;

		Player player = component.getPlayer(playerId);
		int xScroll = (int) (player.pos.x - MojamComponent.GAME_WIDTH / 2);
		int yScroll = (int) (player.pos.y - (MojamComponent.GAME_HEIGHT - 24) / 2);
		Set<Entity> entityList = getEntitiesToSend(component.level, xScroll - Tile.WIDTH, yScroll
				- Tile.HEIGHT, xScroll + MojamComponent.GAME_WIDTH -+Tile.WIDTH, yScroll
				+ MojamComponent.GAME_HEIGHT + Tile.HEIGHT);
		toSend = new ArrayList<Entity>();
		Iterator<Entity> iter = entityList.iterator();
		while (iter.hasNext()) {
			Entity e = iter.next();
			if(e.type > 0 && EntityList.mpTransfer.contains(e.type)){
				toSend.add(e);
			}
		}
	}
	
	public Set<Entity> getEntitiesToSend(Level level, double xx0, double yy0, double xx1,
			double yy1) {
		int x0 = (int) (xx0) / Tile.WIDTH;
		int x1 = (int) (xx1) / Tile.WIDTH;
		int y0 = (int) (yy0) / Tile.HEIGHT;
		int y1 = (int) (yy1) / Tile.HEIGHT;

		Set<Entity> result = new TreeSet<Entity>(new EntityComparator());

		for (int y = y0; y <= y1; y++) {
			if (y < 0 || y >= level.height)
				continue;
			for (int x = x0; x <= x1; x++) {
				if (x < 0 || x >= level.width)
					continue;
				List<Entity> entities = level.entityMap[x + y * level.width];
				for (int i = 0; i < entities.size(); i++) {
					Entity e = entities.get(i);
					if (e.removed)
						continue;
					if (e.intersects(xx0, yy0, xx1, yy1)/* || e.age < 3*/) {
						result.add(e);
					}
				}
			}
		}

		return result;
	}

	@Override
	public void read(DataInputStream dis) throws IOException {
		int size = dis.readShort();
		/*if(component.level != null && System.currentTimeMillis() % 1000 < 30){
			System.out.println("MPDataPacket .. read size "+size);
		}*/
		for(int i = 0; i < size; i++) {
			short id = dis.readShort();
			short type = dis.readShort();
			double xPos = dis.readFloat();
			double yPos = dis.readFloat();
			boolean removed = dis.readBoolean();
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
			if(e.type != type){
				System.out.println("MPDataPacket TYPE MISMATCH: ("+id+" "+type+") ("+e.id+" "+e.type+")");
			}
			e.setId(id);
			e.age = 0;
			e.removed = removed;
			
			/*double xVel = e.pos.x - e.prevPos.x;
	    	double yVel = e.pos.y - e.prevPos.y;
			e.xd = xVel;
			e.yd = yVel;
			 */
			if(e instanceof Mob){
				Mob m = (Mob) e;
				m.setTeam(dis.readShort());
				m.health = dis.readFloat();
				m.isImmortal = dis.readBoolean();
				m.hurtTime = dis.readShort();
				if(m instanceof Bat){
					((Bat) m).setBRSeed(dis.readLong());
				}
			} else if(e instanceof Loot){
				Loot l = (Loot) e;
				l.setValue(dis.readShort());
			} else if(e instanceof Bullet){
				Bullet b = (Bullet) e;
				b.owner = (Mob) EntityList.idToEntityMap.get(dis.readShort());
				if(b.owner == null){
					b.owner = MojamComponent.instance.player; // fixes null when no owner is sent from packet
				}
				b.damage = dis.readFloat();
				b.xa = dis.readFloat();
				b.ya = dis.readFloat();
				double angle = (Math.atan2(b.ya, b.xa) + Math.PI * 1.625);
				b.facing = (8 + (int) (angle / Math.PI * 4)) & 7;
			} else if(e instanceof SpawnerEntity){
				SpawnerEntity s = (SpawnerEntity) e;
				s.type = dis.readShort();
			}

		}

		if(component.level != null && System.currentTimeMillis() % 1000 < 30){
			System.out.println("Recieve done, "+size+" OF "+component.level.entities.size());
		}
	}

	@Override
	public void write(DataOutputStream dos) throws IOException {


		if(System.currentTimeMillis() % 1000 < 30){
			//System.out.println(player.name+": "+mobs.size()+"/"+visibleEntities.size()+" OF "+component.level.entities.size());
		}

		dos.writeShort(toSend.size());
		for(Entity e : toSend){
			dos.writeShort(e.id);
			dos.writeShort(e.type);
			dos.writeFloat((float) e.pos.x);
			dos.writeFloat((float) e.pos.y);
			dos.writeBoolean(e.removed);

			if(e instanceof Mob){
				Mob m = (Mob) e;
				dos.writeShort(m.getTeam());
				dos.writeFloat(m.health);
				dos.writeBoolean(m.isImmortal);
				dos.writeShort(m.hurtTime);
				if(m instanceof Bat){
					dos.writeLong(((Bat) m).getBRSeed());
				}
			} else if(e instanceof Loot){
				Loot l = (Loot) e;
				dos.writeShort(l.getValue());
			} else if(e instanceof Bullet){
				Bullet b = (Bullet) e;
				dos.writeShort(b.owner.id);
				dos.writeFloat(b.damage);
				dos.writeFloat((float) b.xa);
				dos.writeFloat((float) b.ya);
			} else if(e instanceof SpawnerEntity){
				SpawnerEntity s = (SpawnerEntity) e;
				dos.writeShort(s.type);
			}
		}
	}

}
