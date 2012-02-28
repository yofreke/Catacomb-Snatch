package com.mojang.mojam.network;


public class ClientSidePacketLink extends NetworkPacketLink {

	public ClientSidePacketLink(String host, int port) throws Exception {
		super(port);
		this.startWrite(host);
	}

}
