package com.mojang.mojam.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.mojang.mojam.network.NetworkCommand;

public class PlayerPositionCommand extends NetworkCommand {

	private float posX, posY;

	public PlayerPositionCommand() {
	}

	public PlayerPositionCommand(double posX, double posY) {
		this.posX = (float) posX;
		this.posY = (float) posY;
	}

	@Override
	public void read(DataInputStream dis) throws IOException {
		posX = dis.readFloat();
		posY = dis.readFloat();
	}

	@Override
	public void write(DataOutputStream dos) throws IOException {
		dos.writeFloat(posX);
		dos.writeFloat(posY);
	}

	public float getX() {
		return posX;
	}

	public float getY() {
		return posY;
	}
}