package com.mojang.mojam.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import com.mojang.mojam.network.packet.ChangeKeyCommand;
import com.mojang.mojam.network.packet.ChangeMouseButtonCommand;
import com.mojang.mojam.network.packet.ChangeMouseCoordinateCommand;
import com.mojang.mojam.network.packet.ChatPacket;
import com.mojang.mojam.network.packet.HandshakePacket;
import com.mojang.mojam.network.packet.HandshakeResponse;
import com.mojang.mojam.network.packet.MPDataPacket;
import com.mojang.mojam.network.packet.MPPlayerPosPacket;
import com.mojang.mojam.network.packet.MPUpdateIDPacket;
import com.mojang.mojam.network.packet.PlayerUpdatePacket;
import com.mojang.mojam.network.packet.SetPlayerPacket;
import com.mojang.mojam.network.packet.StartGamePacket;
import com.mojang.mojam.network.packet.StartGamePacketCustom;
import com.mojang.mojam.network.packet.StartPregamePacket;
import com.mojang.mojam.network.packet.TurnPacket;

public abstract class Packet {

	public static HashMap<Integer, Class<? extends Packet>> idToClassMap = new HashMap<Integer, Class<? extends Packet>>();
	private static Map<Class<? extends Packet>, Integer> classToIdMap = new HashMap<Class<? extends Packet>, Integer>();

	static void map(int id, Class<? extends Packet> clazz) {
		if (idToClassMap.containsKey(id))
			throw new IllegalArgumentException("Duplicate packet id:" + id);
		if (classToIdMap.containsKey(clazz))
			throw new IllegalArgumentException("Duplicate packet class:"
					+ clazz);
		idToClassMap.put(id, clazz);
		classToIdMap.put(clazz, id);
	}

	static {
		map(10, StartGamePacket.class);
		map(11, TurnPacket.class);
		map(12, StartGamePacketCustom.class);
		map(13, StartPregamePacket.class);
		map(14, PlayerUpdatePacket.class);
		map(15, ChatPacket.class);
		map(16, SetPlayerPacket.class);
		map(17, MPDataPacket.class);
		map(18, MPPlayerPosPacket.class);
		map(19, MPUpdateIDPacket.class);
		map(20, HandshakePacket.class);
		map(21, HandshakeResponse.class);

		map(100, ChangeKeyCommand.class);
		map(101, PauseCommand.class);
		map(104, ChangeMouseButtonCommand.class);
		map(105, ChangeMouseCoordinateCommand.class);
	}

	private InetAddress recvAddress;
	public void setRecvAddress(InetAddress addr) { this.recvAddress = addr; }
	public InetAddress getRecvAddress() {
		if(recvAddress == null){
			return NetworkPacketLink.getServerAddr();
		}
		return recvAddress;
	}
	
	private Client client;
	public Packet setClient(Client client) { 
		this.client = client;
		return this;
	}
	public Client getClient() { return this.client; }
	public int getPort(){
		if(client == null) return NetworkPacketLink.SERVER_PORT;
		return client.port;
	}
	public InetAddress getWriteAddress(){
		if(client == null) return NetworkPacketLink.getServerAddr();
		return client.address;
	}
	
	public final int getId() {
		return classToIdMap.get(getClass());
	}

	public abstract void read(DataInputStream dis) throws IOException;

	public abstract void write(DataOutputStream dos) throws IOException;

	public static void writePacket(Packet packet, DataOutputStream dos)
			throws IOException {
		if(packet.getId() == 0){
			throw(new RuntimeException("BAD PACKET CLASS: "+packet.getClass().getName()));
		}
		dos.write(packet.getId());
		packet.write(dos);
	}

	public static Packet readPacket(DataInputStream inputStream, InetAddress addr)
			throws IOException {

		int id = 0;
		Packet packet = null;

		try {
			id = inputStream.read();
			if (id == -1)
				return null;
			
			packet = getPacket(id);
			if (packet == null)
				throw new IOException("Bad packet id " + id);

			packet.setRecvAddress(addr);
			packet.read(inputStream);

		} catch (EOFException e) {
			// reached end of stream
			System.out.println("Reached end of stream");
			return null;
		}

		return packet;
	}

	public static Packet getPacket(int id) {
		try {
			Class<? extends Packet> clazz = idToClassMap.get(id);
			if (clazz == null)
				return null;
			return clazz.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Skipping packet with id " + id);
			return null;
		}
	}

	public void handle(PacketListener packetListener) {
		packetListener.handle(this);
	}

}
