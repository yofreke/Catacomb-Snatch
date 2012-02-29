package com.mojang.mojam.entity.mob;

import java.util.Random;

import com.mojang.mojam.entity.Entity;
import com.mojang.mojam.level.DifficultyInformation;
import com.mojang.mojam.screen.Art;
import com.mojang.mojam.screen.Bitmap;
import com.mojang.mojam.screen.Screen;

public class Bat extends HostileMob {
	private int tick = 0;
	
	private long brSeed;
	private Random batRand;
	
	public Bat(double x, double y) {
		super(x, y, Team.Neutral);
		setPos(x, y);
		setStartHealth(1);
		dir = rand.nextDouble() * Math.PI * 2;
		minimapColor = 0xffff0000;
		yOffs = 5;
		deathPoints = 1;
		
		batRand = new Random();
		setBRSeed(rand.nextLong());
	}

	public void setBRSeed(long l){
		this.brSeed = l;
		batRand.setSeed(brSeed);
	}
	public long getBRSeed(){
		return brSeed;
	}
	
	public void tick() {
		super.tick();
		if (freezeTime > 0)
			return;

		tick++;
		if(isServer() && tick % 180 == 0){
			setBRSeed(rand.nextLong());
			needSend = true;
		}

		dir += (batRand.nextDouble() - batRand.nextDouble()) * 0.2;
		xd += Math.cos(dir) * 1;
		yd += Math.sin(dir) * 1;
			
		if (shouldBounceOffWall(xd, yd)){
			xd = -xd;
			yd = -yd;
		}
		
		if (!move(xd, yd) && isServer()) {
			dir += (batRand.nextDouble() - batRand.nextDouble()) * 0.8;
			needSend = true;
		}
		xd *= 0.2;
		yd *= 0.2;
	}

	public void die() {
		super.die();
	}

	public Bitmap getSprite() {
		return Art.bat[(tick / 3) & 3][0];
	}

	@Override
	public void render(Screen screen) {
		if (tick % 2 == 0)
			screen.blit(Art.batShadow, pos.x - Art.batShadow.w / 2, pos.y
					- Art.batShadow.h / 2 - yOffs + 16);
		super.render(screen);

	}

	@Override
	public void collide(Entity entity, double xa, double ya) {
		super.collide(entity, xa, ya);

		if (entity instanceof Mob) {
			Mob mob = (Mob) entity;
			if (isNotFriendOf(mob)) {
				mob.hurt(this, DifficultyInformation.calculateStrength(1));
			}
		}
	}
}
