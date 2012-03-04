package com.mojang.mojam.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlayerTurnCommands {

	private HashMap<Integer, List<PlayerCommands>> playerCommands;

	public PlayerTurnCommands(int numPlayers) {
		playerCommands = new HashMap<Integer, List<PlayerCommands>>();
		/*for (int i = 0; i < numPlayers; i++) {
			playerCommands.add(i, new ArrayList<PlayerCommands>());
		}*/
	}

	public boolean isAllDone(int turnNumber) {
		for (List<PlayerCommands> commandList : playerCommands.values()) {
			boolean found = false;
			for (PlayerCommands commands : commandList) {
				if (commands.turnNumber == turnNumber) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}

	public List<PlayerCommands> getList(int playerId){
		if(!playerCommands.containsKey(playerId)){
			playerCommands.put(playerId, new ArrayList<PlayerCommands>());
		}
		return playerCommands.get(playerId);
	}
	
	public void addPlayerCommands(int playerId, int turnNumber,
			List<NetworkCommand> commands) {
		getList(playerId).add(
				new PlayerCommands(turnNumber, commands));
	}

	public List<NetworkCommand> popPlayerCommands(int playerId, int turnNumber) {
		for (PlayerCommands commands : getList(playerId)) {
			if (commands.turnNumber == turnNumber) {
				playerCommands.get(playerId).remove(commands);
				return commands.commands;
			}
		}
		return null;
	}

	private class PlayerCommands {
		private int turnNumber;
		private List<NetworkCommand> commands;

		public PlayerCommands(int turnNumber, List<NetworkCommand> commands) {
			this.turnNumber = turnNumber;
			this.commands = commands;
		}
	}

}
