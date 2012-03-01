package com.mojang.mojam.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.mojang.mojam.level.Level;
import com.mojang.mojam.level.LevelInformation;
import com.mojang.mojam.level.TileID;
import com.mojang.mojam.level.tile.Tile;
import com.mojang.mojam.network.Packet;

public class StartPregamePacket extends Packet {
	
	public short id;
	public Level level;
	public int difficulty;
	public int levelWidth, levelHeight;
	public Tile[] tiles;
	public LevelInformation levelInfo;
	
	public boolean doneRead;
	
	public StartPregamePacket(){
	}
	
	public StartPregamePacket(short id, Level level, int difficulty){
		this.id = id;
		this.level = level;
		this.difficulty = difficulty;
	}
	
	@Override
	public void read(DataInputStream dis) throws IOException {
		id = dis.readShort();
		levelInfo = LevelInformation.readMP(dis);
		levelWidth = dis.readInt();
		levelHeight = dis.readInt();
		
		tiles = new Tile[levelWidth * levelHeight];
		int numTiles = dis.readShort();
		System.out.println("NUM TILES:"+numTiles);
		for(int i = 0; i < numTiles; i++){
			short id = dis.readShort();
			short img = dis.readShort();
			
			tiles[i] = TileID.shortToTile(id);
			if(tiles[i] != null) tiles[i].img = img; 
		}
		this.difficulty = dis.readInt();
		doneRead = true;
	}

	@Override
	public void write(DataOutputStream dos) throws IOException {
		dos.writeShort(id);
		level.getInfo().sendMP(dos);
		dos.writeInt(level.width);
		dos.writeInt(level.height);
		dos.writeShort(level.tiles.length);
		for(int i = 0; i < level.tiles.length; i++){
			dos.writeShort(TileID.tileToShort(level.tiles[i]));
			dos.writeShort(level.tiles[i].img);
		}
		dos.writeInt(difficulty);
	}
	
	public Level getLevel() {
		System.out.println("Pregame packet level dim:"+levelWidth+"x"+levelHeight);
		Level level = new Level(levelWidth, levelHeight, 0,0).setInfo(levelInfo);
		for(int y = 0; y < level.height; y++){
			for(int x = 0; x < level.width; x++){
				int index = x + y * level.width;
				Tile tile = tiles[index];
				tile.level = level;
				tile.x = x;
				tile.y = y;
				level.setTile(x, y, tiles[index]);
			}
		}
		return level;
	}

	public int getDifficulty() {
		return difficulty;
	}
}
