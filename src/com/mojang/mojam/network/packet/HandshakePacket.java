package com.mojang.mojam.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.network.NetworkPacketLink;
import com.mojang.mojam.network.Packet;

public class HandshakePacket extends Packet {

	private String gameName;
	private int port;
	
	public HandshakePacket(){
	}
	
	@Override
	public void read(DataInputStream dis) throws IOException {
		gameName = dis.readUTF();
		port = dis.readInt();
	}

	@Override
	public void write(DataOutputStream dos) throws IOException {
		dos.writeUTF("catacomb_snatch");
		dos.writeInt(NetworkPacketLink.getPort());
	}

	public String getGamename() { return gameName; }
	public int getSendPort() { return port; }
}
