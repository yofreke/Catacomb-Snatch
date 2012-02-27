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

	private static final int SEND_BUFFER_SIZE = 1024 * 5;

	private String host;
	private int port;
	private InetAddress address;
	private DatagramSocket socket;

	private Object writeLock = new Object();

	private List<Packet> incoming = Collections
			.synchronizedList(new ArrayList<Packet>());
	private List<Packet> outgoing = Collections
			.synchronizedList(new ArrayList<Packet>());

	private DataInputStream inputStream;
	private DataOutputStream outputStream;

	private Thread writeThread;
	private Thread readThread;

	private boolean isRunning = true;
	private boolean isQuitting = false;
	private boolean isDisconnected = false;

	private PacketListener packetListener;

	public NetworkPacketLink(String host, int port) throws IOException {
		address = InetAddress.getByName(host);
		this.socket = new DatagramSocket(port);

		/*inputStream = new DataInputStream(socket.getInputStream());
		outputStream = new DataOutputStream(new BufferedOutputStream(
				socket.getOutputStream(), SEND_BUFFER_SIZE));*/

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

		readThread.start();
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

	public void sendPacket(Packet packet) {
		if (isQuitting) {
			return;
		}
		synchronized (writeLock) {
			outgoing.add(packet);
		}
	}

	private boolean readTick() {
		boolean didSomething = false;
		try {
			byte buffer[] = new byte[65535];
			DatagramPacket packet1 = new DatagramPacket (buffer, buffer.length);
			socket.receive (packet1);
			ByteArrayInputStream byteIn = new ByteArrayInputStream (packet1.getData (), 0, packet1.getLength ());
			DataInputStream dataIn = new DataInputStream (byteIn);
			
			Packet packet = Packet.readPacket(dataIn/*inputStream*/);

			if (packet != null) {
				if (!isQuitting) {
					incoming.add(packet);
				}
				didSomething = true;
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
				Packet packet;
				synchronized (writeLock) {
					packet = outgoing.remove(0);
				}
				
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream ();
				DataOutputStream dataOut = new DataOutputStream (byteOut);
				Packet.writePacket(packet, dataOut/*outputStream*/);
				byte[] data = byteOut.toByteArray ();
				DatagramPacket packet1 = new DatagramPacket (data, data.length, address, port);
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

}
