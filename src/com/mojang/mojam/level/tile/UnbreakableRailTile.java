package com.mojang.mojam.level.tile;

public class UnbreakableRailTile extends RailTile {
	
	public UnbreakableRailTile(){ this(new FloorTile()); }
	public UnbreakableRailTile(Tile parent) {
		super(parent);
	}

	public boolean remove() {
		return false;
	}
}
