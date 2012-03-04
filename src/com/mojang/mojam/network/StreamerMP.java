package com.mojang.mojam.network;

import java.util.ArrayList;
import java.util.List;

import com.mojang.mojam.Keys;
import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.entity.Player;
import com.mojang.mojam.network.packet.ChangeKeyCommand;
import com.mojang.mojam.network.packet.ChangeMouseButtonCommand;
import com.mojang.mojam.network.packet.ChangeMouseCoordinateCommand;
import com.mojang.mojam.network.packet.MPDataPacket;
import com.mojang.mojam.network.packet.MPUpdateIDPacket;
import com.mojang.mojam.network.packet.PlayerPositionCommand;
import com.mojang.mojam.network.packet.TurnPacket;

public class StreamerMP {
	
	public static final int SEND_RATE = 5;
	
	private int tick = 0;
	private MojamComponent component;
	ArrayList<NetworkCommand> localPlayerCommands = new ArrayList<NetworkCommand>();
	private PlayerTurnCommands playerCommands;
	private CommandListener commandListener;
	
	public StreamerMP(MojamComponent mc){
		this.component = mc;
		playerCommands = new PlayerTurnCommands(2);
		commandListener = component;
	}
	
	public void tick(boolean isKeyframe){
		
		if(tick-- == 0 || isKeyframe){
			tick = SEND_RATE;
			if(component.isServer){				
				for (int i = 0; i < component.level.players.size(); i++) {
					Player player = component.level.players.get(i);
					if(player.id == component.player.id) continue;
					sendToPlayer(player, isKeyframe);
				}
			} else {
				sendFromPlayer(component.player);
			}
		}
		
		long curTime = System.currentTimeMillis();
		if(component.isServer && curTime - NetworkPacketLink.lastReset > 1000){
			NetworkPacketLink.lastReset = curTime;
			MojamComponent.instance.chatWindow.addMessage("SYS: "+
					(int) Math.floor(NetworkPacketLink.sentDataSize*0.0009765625)+" KB/s");
			NetworkPacketLink.sentDataSize = 0;
		}
		
		if(component.isServer){
			MPUpdateIDPacket.tick(component);
		} else if(addPlayerCommands(component.player)){
			sendPlayerCommands(component.player);
		}
		
		for (Player p : component.playerMap.values()) {
			List<NetworkCommand> commands = playerCommands.popPlayerCommands(p.id, 0);
			if (commands != null) {
				for (NetworkCommand command : commands) {
					commandListener.handle(p.id, command);
				}
			}
		}
	}
	
	public synchronized void onTurnPacket(TurnPacket packet) {
		playerCommands.addPlayerCommands(packet.getPlayerId(),
				packet.getTurnNumber(), packet.getPlayerCommandList());
	}
	
	public boolean addPlayerCommands(Player player){
		if(player == null) return false;
		boolean flag = false;
		
		for (int index = 0; index < player.mouseButtons.currentState.length; index++) {
			boolean nextState = player.mouseButtons.nextState[index];
			if (player.mouseButtons.isDown(index) != nextState) {
				addCommand(new ChangeMouseButtonCommand(index,nextState));
				flag = true;
			}
		}
		
		for (int index = 0; index < player.keys.getAll().size(); index++) {
			Keys.Key key = player.keys.getAll().get(index);
			boolean nextState = key.nextState;
			if (key.isDown != nextState) {
				//System.out.println("client sendkey: "+key.name+":"+key.nextState);
				addCommand(new ChangeKeyCommand(index, nextState));
				flag = true;
			}
		}
		return flag;
	}
	
	public void sendFromPlayer(Player player){
		if(component.packetLink == null || component.player == null) return;
		sendPlayerCommands(component.player);
		//component.packetLink.sendPacket(new MPPlayerPosPacket(component.player));		
	}
	public void sendPlayerCommands(Player player){
		addCommand(new ChangeMouseCoordinateCommand(player.mouseButtons.getX(), 
				player.mouseButtons.getY(), player.mouseButtons.mouseHidden));
		
		addCommand(new PlayerPositionCommand(component.player.pos.x, component.player.pos.y));
		component.packetLink.sendPacket(new TurnPacket(component.getLocalId(), 0, localPlayerCommands));
		
		localPlayerCommands.clear();
	}
	public void addCommand(NetworkCommand cmd){
		localPlayerCommands.add(cmd);
	}
	
	public void sendToPlayer(Player player, boolean keyframe){
		if(player == null || component.isPaused()) return;
		MPDataPacket packet = new MPDataPacket(component, player.id, keyframe);
		component.packetLink.sendPacket(packet.setClient(player.client));
	}
}
