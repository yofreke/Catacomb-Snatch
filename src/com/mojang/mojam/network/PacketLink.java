package com.mojang.mojam.network;

public interface PacketLink {

	public void sendPacket(Packet packet);
	public void sendPacket(SendablePacket packet);

	public void tick();

	public void setPacketListener(PacketListener packetListener);

}
