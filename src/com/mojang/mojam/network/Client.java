package com.mojang.mojam.network;

import java.net.InetAddress;

public class Client {
	
	public InetAddress address;
	public int port;
	public int id;
	
	public Client(InetAddress addr, int port, int id){
		this.address = addr;
		this.port = port;
		this.id = id;
	}
}
