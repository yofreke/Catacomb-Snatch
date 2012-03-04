package com.mojang.mojam.entity.loot;

import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.entity.Entity;
import com.mojang.mojam.entity.Player;
import com.mojang.mojam.level.tile.HoleTile;
import com.mojang.mojam.screen.Art;
import com.mojang.mojam.screen.Bitmap;
import com.mojang.mojam.screen.Screen;

public class Loot extends Entity {
	public double xMovement, yMovement, accelerationDirectionDelta;
	private double prevXMovement, prevYMovement;
	public double accelerationDirection;
	public Entity owner;
	public int life;
	public int animationTime = 0;
	private int value = 0;
	public boolean fake = false;
	private boolean isTakeable;
	private boolean disappears = true; 

	public static Bitmap[][][] animationArt = {
		Art.pickupCoinBronzeSmall,
		Art.pickupCoinSilverSmall,
		Art.pickupCoinGoldSmall,
		Art.pickupCoinBronze,
		Art.pickupCoinSilver,
		Art.pickupCoinGold,
		Art.pickupGemEmerald,
		Art.pickupGemRuby,
		Art.pickupGemDiamond};

	public static int[] values = { 1, 2, 5, 10, 20, 50, 100, 200, 500};

public Loot(double x, double y){
		this(x, y, 0,0,0);
	}
	
	public Loot(double x, double y, double xDirection, double yDirection, int lootValue, boolean disappears) {
		setup(x, y, xDirection, yDirection, lootValue, disappears);
	}

	public Loot(double x, double y, double xDirection, double yDirection, int lootValue) {
		setup(x, y, xDirection, yDirection, lootValue, true);
	}
	
	public void setup(double x, double y, double xDirection, double yDirection, int lootValue, boolean disappears){
		pos.set(x, y);
		isTakeable = true;

		value = 0;
		while (value < 8 && values[value] < lootValue)
			value++;

		if(isServer()){
			if (rand.nextInt(3) == 0)
				value++;
			if (rand.nextInt(3) == 0)
				value++;
			if (value > 8)
				value = 8;
		}
		
		double power = rand.nextDouble() * 1 + 1;		this.xMovement = xDirection * power;		this.yMovement = yDirection * power;		this.accelerationDirectionDelta = rand.nextDouble() * 2 + 1.0;				this.setSize(2, 2);
		this.disappears=disappears;
		physicsSlide = false;
		life = rand.nextInt(100) + 600;

		animationTime = rand.nextInt(animationArt[value].length * 3);

	}
	
	public void makeUntakeable() {
		isTakeable = false;
		life = 100 - rand.nextInt(40);
	}

	public void tick() {
		if (level.getTile(pos) instanceof HoleTile) {
			remove();
		}
		
		animationTime++;
		move(xMovement, yMovement);
		accelerationDirection += accelerationDirectionDelta;
		if (accelerationDirection < 0) {
			accelerationDirection = 0;
			xMovement *= 0.8;
			yMovement *= 0.8;
		} else {
			xMovement *= 0.98;
			yMovement *= 0.98;
		}
		
		accelerationDirectionDelta -= 0.2;
		if (this.disappears){
			if (--life < 0)
				remove();
		}
		
		if (isTakeable && isServer()) {
			double fixDistance = 100;
			int absorbDistance = 16;
			for (Entity entity : level.getEntities(getBB().grow(fixDistance))) {
				if (!(entity instanceof LootCollector))
					continue;
				LootCollector collector = (LootCollector) entity;
				double xDelta = entity.pos.x - pos.x;
				double yDelta = entity.pos.y - pos.y;
				double distance = Math.sqrt(xDelta * xDelta + yDelta * yDelta);
				if (!collector.canTake()) {
					double localDistance = 80;
					if (xDelta * xDelta + yDelta * yDelta < localDistance * localDistance) {
						if (distance < absorbDistance) {
							onTake(collector);
							return;
						}
						xDelta /= distance;
						yDelta /= distance;
						double power = (1 - (distance / localDistance)) * 0.1;
						if (accelerationDirection <= 0) {
							xMovement -= xDelta * power;
							yMovement -= yDelta * power;
						}
					}
				} else {
					double suckPower = collector.getSuckPower();
					double localDistance = (fixDistance - 40) * suckPower + 40;
					if (xDelta * xDelta + yDelta * yDelta < localDistance * localDistance) {
						collector.notifySucking();
						if (distance < absorbDistance) {
							onTake(collector);
							return;
						}
						xDelta /= distance;
						yDelta /= distance;
						double power = (1 - (distance / localDistance)) * 1.6 * (suckPower * 0.5 + 0.5);
						if (accelerationDirection <= 0) {
							xMovement += xDelta * power;
							yMovement += yDelta * power;
						}
					}
				}
			}
		}
		
		double d = 0.01;
		if(Math.abs(prevXMovement-xMovement) > d || Math.abs(prevYMovement-yMovement) > d){
			prevXMovement = xMovement;
			prevYMovement = yMovement;
			needSend = true;
		}
	}

	public void forceTake(LootCollector taker) {
		onTake(taker);
	}

	protected void onTake(LootCollector taker) {
		remove();
		taker.flash();
		taker.take(this);

		if (value > 8) {
			MojamComponent.soundPlayer.playSound("/sound/Big Gem.wav",
					(float) pos.x, (float) pos.y);
		} else if (value > 6) {
			MojamComponent.soundPlayer.playSound("/sound/Gem.wav",
					(float) pos.x, (float) pos.y);
		} else if (value > 4) {
			MojamComponent.soundPlayer.playSound("/sound/Big Coin.wav",
					(float) pos.x, (float) pos.y);
		} else {
			MojamComponent.soundPlayer.playSound("/sound/Small Coin.wav",
					(float) pos.x, (float) pos.y);
		}
	}

	protected boolean shouldBlock(Entity e) {
		return false;
	}

	@Override
	public void handleCollision(Entity entity, double xa, double ya) {
		if (isTakeable && entity instanceof Player) {
			((Player) entity).take(this);
		}
	}

	public void render(Screen screen) {
		Bitmap[][] lootAnimation = animationArt[value];
		if (life > 60 * 3 || life / 2 % 2 == 0) {
			int frame = animationTime / 3 % lootAnimation.length;
			Bitmap currentFrame = lootAnimation[frame][0];
			if (accelerationDirection > 0) {
				screen.blit(Art.shadow, pos.x - 2, pos.y);
			}
			screen.blit(currentFrame, pos.x - currentFrame.w / 2, pos.y - currentFrame.h / 2 - 2 - accelerationDirection);
		}
	}

	public int getScoreValue() {
		return fake ? 0 : values[value];
	}
	
	public int getValue(){
		return value;
	}
	public void setValue(int i){
		value = i;
	}
}