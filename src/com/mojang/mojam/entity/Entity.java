package com.mojang.mojam.entity;

import java.util.List;
import java.util.Random;

import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.entity.animation.LargeBombExplodeAnimation;
import com.mojang.mojam.level.Level;
import com.mojang.mojam.level.tile.Tile;
import com.mojang.mojam.math.BB;
import com.mojang.mojam.math.BBOwner;
import com.mojang.mojam.math.Vec2;
import com.mojang.mojam.screen.Art;
import com.mojang.mojam.screen.Screen;

public abstract class Entity implements BBOwner {
	
	protected Random rand = new Random();
	// 0-1000 Players
	// 1000-15000 Server
	// 15000-32000 Client
	private static short idCounter_c = 15000, idCounter_s = 1000;
	private static short getNewId(){
		short i;
		if(MojamComponent.instance.isServer()){
			i = idCounter_s;
			if(++idCounter_s > 15000){
				idCounter_s = (short) (1000);
			}
		} else {
			i = idCounter_c;
			if(++idCounter_c > 32000){
				idCounter_c = (short) (15000);
			}
		}
		return i;
	}
	public short id;
	public short type;
	public int age;
	
	public Level level;
	public boolean removed;
	public Vec2 pos = new Vec2(0, 0);
	public Vec2 prevPos = new Vec2(0, 0);
	public Vec2 radius = new Vec2(10, 10);

	public boolean isBlocking = true;
	public boolean physicsSlide = true;

	public int xto;
	public int yto;
	public double xd, yd; // velocity
	public double prevXd, prevYd; // velocity
	public int minimapIcon = -1;
	public int minimapColor = -1;

	public Entity(){
		this.type = EntityList.classToShort(getClass());
	}
	
	public void setPos(double x, double y) {
		pos.set(x, y);
	}
	
	public void setPos(Vec2 position) {
	    pos.x = position.x;
	    pos.y = position.y;
    }

	public void setSize(int xr, int yr) {
		radius.set(xr, yr);
	}
	
	public void setId(){
		if(id == 0){
			setId(getNewId());
		}
	}
	public void setId(short newId){
		if(id == newId) return;
		Entity e = EntityList.idToEntityMap.get(newId);
		if(e != null && !e.equals(this)){
			System.out.println("ID conflict: "+id+"->"+newId+" "+MojamComponent.cleanClassName(this.getClass().getName())
					+","+MojamComponent.cleanClassName(e.getClass().getName()));
			e.id = 0;
			e.setId();
		}
		EntityList.idToEntityMap.put(this.id, null);
		/*if(MojamComponent.instance.isServer){
			MPUpdateIDPacket.addIdChange(id, newId);
		}*/
		this.id = newId;
		EntityList.idToEntityMap.put(this.id, this);
	}

	public final void init(Level level) {
		this.level = level;
		setId();
		init();
	}

	public void init() {
	}

	public void tick() {
	}

	public void postTick(){
		prevXd = xd;
		prevYd = yd;
		prevPos.set(pos.x, pos.y);

		if(!isServer() && age++ > 2 && id < 15000 && this.id != MojamComponent.instance.player.id){
			remove();
		}
	}
	
	public boolean intersects(double xx0, double yy0, double xx1, double yy1) {
		return getBB().intersects(xx0, yy0, xx1, yy1);
	}

	public BB getBB() {
		return new BB(this, pos.x - radius.x, pos.y - radius.y, pos.x + radius.x, pos.y + radius.y);
	}

	public void render(Screen screen) {
		screen.blit(Art.floorTiles[3][0], pos.x - Tile.WIDTH / 2, pos.y - Tile.HEIGHT / 2 - 8);
	}

	protected boolean move(double xa, double ya) {
		List<BB> bbs = level.getClipBBs(this);
		if (physicsSlide) {
			boolean moved = false;
			if (!removed)
				moved |= partMove(bbs, xa, 0);
			if (!removed)
				moved |= partMove(bbs, 0, ya);
			return moved;
		} else {
			boolean moved = true;
			if (!removed)
				moved &= partMove(bbs, xa, 0);
			if (!removed)
				moved &= partMove(bbs, 0, ya);
			return moved;
		}
	}

	private boolean partMove(List<BB> bbs, double xa, double ya) {
		double oxa = xa;
		double oya = ya;
		BB from = getBB();

		BB closest = null;
		double epsilon = 0.01;
		for (int i = 0; i < bbs.size(); i++) {
			BB to = bbs.get(i);
			if (from.intersects(to))
				continue;

			if (ya == 0) {
				if (to.y0 >= from.y1 || to.y1 <= from.y0)
					continue;
				if (xa > 0) {
					double xrd = to.x0 - from.x1;
					if (xrd >= 0 && xa > xrd) {
						closest = to;
						xa = xrd - epsilon;
						if (xa < 0)
							xa = 0;
					}
				} else if (xa < 0) {
					double xld = to.x1 - from.x0;
					if (xld <= 0 && xa < xld) {
						closest = to;
						xa = xld + epsilon;
						if (xa > 0)
							xa = 0;
					}
				}
			}

			if (xa == 0) {
				if (to.x0 >= from.x1 || to.x1 <= from.x0)
					continue;
				if (ya > 0) {
					double yrd = to.y0 - from.y1;
					if (yrd >= 0 && ya > yrd) {
						closest = to;
						ya = yrd - epsilon;
						if (ya < 0)
							ya = 0;
					}
				} else if (ya < 0) {
					double yld = to.y1 - from.y0;
					if (yld <= 0 && ya < yld) {
						closest = to;
						ya = yld + epsilon;
						if (ya > 0)
							ya = 0;
					}
				}
			}
		}
		if (closest != null && closest.owner != null) {
			closest.owner.handleCollision(this, oxa, oya);
		}
		if (xa != 0 || ya != 0) {
			pos.x += xa;
			pos.y += ya;
			return true;
		}
		return false;
	}

	public final boolean blocks(Entity e) {
		return isBlocking && e.isBlocking && shouldBlock(e) && e.shouldBlock(this);
	}

	protected boolean shouldBlock(Entity e) {
		return true;
	}

	public void remove() {
		removed = true;
		EntityList.idToEntityMap.put((short) this.id, null);
	}

	@Override
	public void handleCollision(Entity entity, double xa, double ya) {
		if (this.blocks(entity)) {
			this.collide(entity, xa, ya);
			entity.collide(this, -xa, -ya);
		}
	}

	public void collide(Entity entity, double xa, double ya) {
	}

	public void hurt(Bullet bullet) {
	}

	public void bomb(LargeBombExplodeAnimation largeBombExplodeAnimation) {
	}

	public boolean isServer(){
		return MojamComponent.instance.isServer();
	}
}