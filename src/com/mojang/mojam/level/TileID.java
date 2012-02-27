package com.mojang.mojam.level;

import java.util.HashMap;

import javax.management.RuntimeErrorException;

import com.mojang.mojam.entity.building.TreasurePile;
import com.mojang.mojam.entity.mob.Team;
import com.mojang.mojam.level.tile.*;

public class TileID {
	
	private static HashMap<Short, Class<? extends Tile>> shortToTileMap = new HashMap<Short, Class<? extends Tile>>();
	private static HashMap<Class<? extends Tile>, Short> tileToShortMap = new HashMap<Class<? extends Tile>, Short>();
	private static HashMap<Integer, Class<? extends Tile>> colorToTileMap = new HashMap<Integer, Class<? extends Tile>>();
	private static HashMap<Class<? extends Tile>, Integer> tileToColorMap = new HashMap<Class<? extends Tile>, Integer>();
	
	static {
		registerTile((short) 0, FloorTile.class, 0xffffff);
		registerTile((short) 1, HoleTile.class, 0x000000);
		registerTile((short) 2, RailTile.class, 0x767676);
		registerTile((short) 3, SandTile.class, 0xA8A800);
		registerTile((short) 4, UnbreakableRailTile.class, 0x969696);
		registerTile((short) 5, UnpassableSandTile.class, 0x888800);
		registerTile((short) 6, WallTile.class, 0xff0000);
		registerTile((short) 7, DestroyableWallTile.class, 0xff7777);
	}
	
	/**
	 * This must be called once so that tiles can be sent via- multiplayer.
	 * They will need a constructor with no arguments
	 */
	public static void registerTile(short id, Class<? extends Tile> tileclass, int color){
		shortToTileMap.put(id, tileclass);
		tileToShortMap.put(tileclass, id);
		colorToTileMap.put(color, tileclass);
		tileToColorMap.put(tileclass, color);
	}
	
	public static short tileToShort(Tile tile){
		if(!tileToShortMap.containsKey(tile.getClass())) return 0;
		return tileToShortMap.get(tile.getClass());
	}
	
	public static Tile shortToTile(short i){
		return classToTile(shortToTileMap.get(i));
	}
	
	public static Tile classToTile(Class<? extends Tile> tileclass){
		Tile tile = null;
		try
        {
            if (tileclass != null)
            {
                tile = (Tile)tileclass.getConstructor().newInstance();
            }
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        return tile;
	}
	
	public static Tile colorToTile(int col){
		Tile tile = null;
		if (col != 0xffff00) {
			tile = classToTile(colorToTileMap.get(col));
		} else return null;
		if(tile == null){
			throw(new RuntimeException("BAD TILE COLOR: "+col));
		}
		return tile;
	}
	
	public static int tileToColor(Tile tile){
		return tileToColorMap.get(tile.getClass());
	}	
}
