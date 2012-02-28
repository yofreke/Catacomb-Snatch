package com.mojang.mojam.network.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.mojang.mojam.network.Packet;

public class HandshakeResponse extends Packet {

	private boolean accepted;
	private byte response;
	private String[] responses = new String[]{
			"Denied",
			"Server Full"
	};
	
	public HandshakeResponse() {}
	public HandshakeResponse(boolean accepted, byte response){
		this.accepted = accepted;
		this.response = response;
	}
	
	@Override
	public void read(DataInputStream dis) throws IOException {
		accepted = dis.readBoolean();
		response = dis.readByte();
	}

	@Override
	public void write(DataOutputStream dos) throws IOException {
		dos.writeBoolean(accepted);
		dos.writeByte(response);
	}
	
	public boolean isAccepted() {
		return accepted;
	}
	public String getReason() {
		return responses[response];
	}
	public byte getResponse() { return response; }
}
