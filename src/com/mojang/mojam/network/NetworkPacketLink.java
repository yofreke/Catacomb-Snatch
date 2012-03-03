package com.mojang.mojam.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.mojam.MojamComponent;

public class NetworkPacketLink implements PacketLink {

	public static final int BUFF_SMALL = 1024 * 5;
	public static final int BUFF_BIG = 1024 * 24;
	
	public static volatile int SEND_BUFFER_SIZE = 512;//1024 * 6;
	public static final int SERVER_PORT = 3000;
	private static InetAddress serverAddress;
	private static int RECV_PORT;
	
	private String host;
	private DatagramSocket socket;

	private Object writeLock = new Object();

	private List<Packet> incoming = Collections
			.synchronizedList(new ArrayList<Packet>());
	private List<SendablePacket> outgoing = Collections
			.synchronizedList(new ArrayList<SendablePacket>());

	private DataInputStream inputStream;
	private DataOutputStream outputStream;

	private Thread writeThread;
	private Thread readThread;

	private boolean isRunning = true;
	private boolean isQuitting = false;
	private boolean isDisconnected = false;

	private PacketListener packetListener;
	
	public static int sentDataSize = 0;
	public static long lastReset = System.currentTimeMillis();
	
	public NetworkPacketLink(int port) throws IOException {
		if(port == 0)this.socket = new DatagramSocket();
		else this.socket = new DatagramSocket(port);
		RECV_PORT = socket.getLocalPort();
		startRead();
	}
	
	public void startRead(){
		System.out.println("Starting read thread: "+RECV_PORT);
		readThread = new Thread("Read thread") {
			public void run() {
				try {
					while (isRunning && !isQuitting) {
						while (readTick())
							;

						try {
							sleep(2L);
						} catch (InterruptedException e) {
						}
					}
				} catch (Exception e) {
				}
			}
		};
		readThread.start();
	}
	public void startWrite(String host) throws Exception {
		this.host = host;
		serverAddress = InetAddress.getByName(host);
		
		System.out.println("Starting write thread: "+host);
		writeThread = new Thread("Write thread") {
			public void run() {
				try {
					while (isRunning) {
						while (writeTick())
							;

						try {
							if (outputStream != null)
								outputStream.flush();
						} catch (IOException e) {
							e.printStackTrace();
							break;
						}

						try {
							sleep(2L);
						} catch (InterruptedException e) {
						}
					}
				} catch (Exception e) {
				}
				MojamComponent.instance.showError("Partner disconnected");
			}
		};
		writeThread.start();
	}

	public void tick() {
		int max = 1000;
		while (!incoming.isEmpty() && max-- >= 0) {
			Packet packet = incoming.remove(0);
			if (packetListener != null) {
				packet.handle(packetListener);
			}
		}
	}

	public void sendPacket(Packet packet){
		sendPacket(new SendablePacket(packet, packet.getWriteAddress(), packet.getPort()));
	}
	public void sendPacket(Packet packet, InetAddress addr, int port){
		sendPacket(new SendablePacket(packet, addr, port));
	}
	
	public void sendPacket(SendablePacket sendablepacket) {
		if (isQuitting) {
			return;
		}
		synchronized (writeLock) {
			outgoing.add(sendablepacket);
		}
	}

	private boolean readTick() {
		boolean didSomething = false;
		try {
			byte buffer[] = new byte[SEND_BUFFER_SIZE];
			DatagramPacket packet1 = new DatagramPacket (buffer, buffer.length);
			socket.receive (packet1);
			ByteArrayInputStream byteIn = new ByteArrayInputStream (packet1.getData (), 0, packet1.getLength ());
			DataInputStream dataIn = new DataInputStream (byteIn);
			
			// read packet
			Packet packet = Packet.readPacket(dataIn/*inputStream*/, packet1.getAddress());
			
			if (packet != null) {
				//System.out.println("PACKET IN: "+packet.getId());
				if (!isQuitting) {
					incoming.add(packet);
				}
				didSomething = true;
			} else {
				System.out.println("null packet (nom nom... MOAR BUFFER)");
			}
		} catch (Exception e) {
			if (!isDisconnected)
				handleException(e);
			return false;
		}
		return didSomething;
	}

	private boolean writeTick() {
		boolean didSomething = false;
		try {
			if (!outgoing.isEmpty()) {
				SendablePacket sendablepacket;
				synchronized (writeLock) {
					sendablepacket = outgoing.remove(0);
				}
				
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream ();
				DataOutputStream dataOut = new DataOutputStream (byteOut);
				Packet.writePacket(sendablepacket.packet, dataOut/*outputStream*/);
				byte[] data = byteOut.toByteArray ();
				sentDataSize += data.length;
				DatagramPacket packet1 = new DatagramPacket (data, data.length,
						sendablepacket.address, sendablepacket.port);
				//System.out.println("PACKET OUT: "+sendablepacket.packet.getId()+" "+packet1.getAddress()+":"+packet1.getPort());
				socket.send(packet1);
				
				didSomething = true;
			}
		} catch (Exception e) {
			if (!isDisconnected)
				handleException(e);
			return false;
		}
		return didSomething;
	}

	private void handleException(Exception e) {
		e.printStackTrace();
		isDisconnected = true;
		socket.close();
	}

	public void setPacketListener(PacketListener packetListener) {
		this.packetListener = packetListener;
	}

	public void close(){
		if(socket != null) socket.close();
	}
	
	public static int getPort() { return RECV_PORT; }
	public static InetAddress getServerAddr() { return serverAddress; }
}
