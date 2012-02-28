package com.mojang.mojam.network;

import java.util.ArrayList;

import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.entity.Player;
import com.mojang.mojam.network.packet.MPDataPacket;
import com.mojang.mojam.network.packet.MPPlayerPosPacket;
import com.mojang.mojam.network.packet.MPUpdateIDPacket;
import com.mojang.mojam.network.packet.TurnPacket;

public class StreamerMP {
	
	private int tick = 0;
	private MojamComponent component;
	ArrayList<NetworkCommand> localPlayerCommands = new ArrayList<NetworkCommand>();
	private PlayerTurnCommands playerCommands;
	private final CommandListener commandListener;
	
	public StreamerMP(MojamComponent mc){
		this.component = mc;
		commandListener = component;
		playerCommands = new PlayerTurnCommands(2);
	}
	
	public void tick(){
		
		if(tick-- <= 0){
			tick = 0;
			if(component.isServer){				
				for (int i = 0; i < component.level.players.size(); i++) {
					Player player = component.level.players.get(i);
					if(player.id == component.player.id) continue;
					sendToPlayer(player);
				}
			} else {
				sendFromPlayer(component.player);
			}
		}
		
		if(component.isServer){
			MPUpdateIDPacket.tick(component);
		}
	}
	
	public synchronized void onTurnPacket(TurnPacket packet) {
		playerCommands.addPlayerCommands(packet.getPlayerId(),
				packet.getTurnNumber(), packet.getPlayerCommandList());
	}
	
	public void sendFromPlayer(Player player){
		if(component.packetLink == null || component.player == null) return;
		
		/*for (int index = 0; index < component.mouseButtons.currentState.length; index++) {
			boolean nextState = component.mouseButtons.nextState[index];
			if (component.mouseButtons.isDown(index) != nextState) {
				addCommand(new ChangeMouseButtonCommand(index,nextState));
			}
		}
		
		addCommand(new ChangeMouseCoordinateCommand(component.mouseButtons.getX(), 
				component.mouseButtons.getY(), component.mouseButtons.mouseHidden));
		
		for (int index = 0; index < component.keys.getAll().size(); index++) {
			Keys.Key key = component.keys.getAll().get(index);
			boolean nextState = key.nextState;
			if (key.isDown != nextState) {
				System.out.println("client sendkey: "+key.name+":"+key.nextState);
				addCommand(new ChangeKeyCommand(index, nextState));
			}
		}
		
		component.packetLink.sendPacket(new TurnPacket(component.getLocalId(), 0, localPlayerCommands));
		
		localPlayerCommands.clear();*/
		component.packetLink.sendPacket(new MPPlayerPosPacket(component.player));		
	}
	public void addCommand(NetworkCommand cmd){
		localPlayerCommands.add(cmd);
	}
	
	public void sendToPlayer(Player player){
		if(player == null || component.isPaused()) return;
		MPDataPacket packet = new MPDataPacket(component, player.id);
		component.packetLink.sendPacket(packet.setClient(player.client));
	}
}
