package com.mojang.mojam.network;

import java.net.InetAddress;


public class SendablePacket {
	
	public InetAddress address;
	public int port;
	public Packet packet;
	
	public SendablePacket(Packet packet, InetAddress addr, int port){
		this.packet = packet;
		this.address = addr;
		this.port = port;
	}
}
