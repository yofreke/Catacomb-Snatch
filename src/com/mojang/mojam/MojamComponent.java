package com.mojang.mojam;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.mojang.mojam.entity.Entity;
import com.mojang.mojam.entity.Player;
import com.mojang.mojam.entity.building.Base;
import com.mojang.mojam.entity.mob.Team;
import com.mojang.mojam.gui.Button;
import com.mojang.mojam.gui.ButtonListener;
import com.mojang.mojam.gui.CharacterSelectionMenu;
import com.mojang.mojam.gui.ClickableComponent;
import com.mojang.mojam.gui.CreditsScreen;
import com.mojang.mojam.gui.DifficultySelect;
import com.mojang.mojam.gui.Font;
import com.mojang.mojam.gui.GuiChatWindow;
import com.mojang.mojam.gui.GuiError;
import com.mojang.mojam.gui.GuiMenu;
import com.mojang.mojam.gui.GuiPregame;
import com.mojang.mojam.gui.HowToPlayMenu;
import com.mojang.mojam.gui.JoinGameMenu;
import com.mojang.mojam.gui.KeyBindingsMenu;
import com.mojang.mojam.gui.LevelEditorMenu;
import com.mojang.mojam.gui.LevelSelect;
import com.mojang.mojam.gui.OptionsMenu;
import com.mojang.mojam.gui.PauseMenu;
import com.mojang.mojam.gui.TitleMenu;
import com.mojang.mojam.gui.WinMenu;
import com.mojang.mojam.level.DifficultyList;
import com.mojang.mojam.level.Level;
import com.mojang.mojam.level.LevelInformation;
import com.mojang.mojam.level.LevelList;
import com.mojang.mojam.level.gamemode.GameMode;
import com.mojang.mojam.level.tile.Tile;
import com.mojang.mojam.mc.EnumOS2;
import com.mojang.mojam.mc.EnumOSMappingHelper;
import com.mojang.mojam.network.Client;
import com.mojang.mojam.network.ClientSidePacketLink;
import com.mojang.mojam.network.CommandListener;
import com.mojang.mojam.network.NetworkCommand;
import com.mojang.mojam.network.NetworkPacketLink;
import com.mojang.mojam.network.Packet;
import com.mojang.mojam.network.PacketListener;
import com.mojang.mojam.network.PauseCommand;
import com.mojang.mojam.network.StreamerMP;
import com.mojang.mojam.network.TurnSynchronizer;
import com.mojang.mojam.network.packet.ChangeKeyCommand;
import com.mojang.mojam.network.packet.ChangeMouseButtonCommand;
import com.mojang.mojam.network.packet.ChangeMouseCoordinateCommand;
import com.mojang.mojam.network.packet.ChatCommand;
import com.mojang.mojam.network.packet.PingPacket;
import com.mojang.mojam.network.packet.ChatPacket;
import com.mojang.mojam.network.packet.HandshakePacket;
import com.mojang.mojam.network.packet.HandshakeResponse;
import com.mojang.mojam.network.packet.PlayerUpdatePacket;
import com.mojang.mojam.network.packet.SetPlayerPacket;
import com.mojang.mojam.network.packet.StartGamePacket;
import com.mojang.mojam.network.packet.StartGamePacketCustom;
import com.mojang.mojam.network.packet.StartPregamePacket;
import com.mojang.mojam.network.packet.TurnPacket;
import com.mojang.mojam.resources.Texts;
import com.mojang.mojam.screen.Art;
import com.mojang.mojam.screen.Bitmap;
import com.mojang.mojam.screen.Screen;
import com.mojang.mojam.sound.SoundPlayer;

public class MojamComponent extends Canvas implements Runnable,
		MouseMotionListener, CommandListener, PacketListener, MouseListener,
		ButtonListener, KeyListener {

	public static final String GAME_TITLE = "Catacomb Snatch";
	public static final String GAME_VERSION = "1.0.0-SNAPSHOT";
	private static Random rand = new Random();
	public static MojamComponent instance;
	public static Locale locale;
	public static Texts texts;
	private static final long serialVersionUID = 1L;
	public static final int GAME_WIDTH = 512;
	public static final int GAME_HEIGHT = GAME_WIDTH * 3 / 4;
	public static final int SCALE = 2;
	public static final int SERVERSTATE_NONE = 0,
		SERVERSTATE_SENDPREGAME = 1,
		SERVERSTATE_PREGAME = 2,
		SERVERSTATE_STARTGAME = 3,
		SERVERSTATE_RUNGAME = 4,
		SERVERSTATE_SGVANILLA = 5;
	public static final int GAMESTATE_PREGAME = 1,
		GAMESTATE_INGAME = 0;
	private static JFrame guiFrame;
	private boolean running = true;
	private boolean paused;
	private Cursor emptyCursor;
	private double framerate = 60;
	private int fps;
	public static Screen screen = new Screen(GAME_WIDTH, GAME_HEIGHT);
	public Level level;
	private Chat chat = new Chat();

	// Latency counter
	private static final int CACHE_EMPTY=0, CACHE_PRIMING=1, CACHE_PRIMED=2;
	private static final int CACHE_SIZE = 5;
	private int latencyCacheState = CACHE_EMPTY;
	private int nextLatencyCacheIdx = 0;
	private int[] latencyCache = new int[CACHE_SIZE];

	private Stack<GuiMenu> menuStack = new Stack<GuiMenu>();

	private InputHandler inputHandler;
	private boolean mouseMoved = false;
	private int mouseHideTime = 0;
	public MouseButtons mouseButtons = new MouseButtons();
	public Keys keys = new Keys();
	//public Keys[] synchedKeys = { new Keys(), new Keys() };
	//public MouseButtons[] synchedMouseButtons = {new MouseButtons(), new MouseButtons() };
	//public Player[] players = new Player[2];
	public HashMap<Short, Player> playerMap = new HashMap<Short, Player>();
	public Player player;
	public TurnSynchronizer synchronizer;
	public NetworkPacketLink packetLink;
	private ServerSocket serverSocket;
	private boolean isMultiplayer;
	public boolean isServer;
	private int localId;
	public static int localTeam; //local team is the team of the client. This can be used to check if something should be only rendered on one person's screen
	
	public int playerCharacter;
	public int opponentCharacter;
	
	private Thread hostThread;
	private static boolean fullscreen = false;
	public static SoundPlayer soundPlayer;
	private long nextMusicInterval = 0;
	private byte sShotCounter = 0;
	private int gameState = GAMESTATE_PREGAME;

	private int createServerState = 0;
	private static File mojamDir = null;
	public GuiChatWindow chatWindow = new GuiChatWindow(this);
	public StreamerMP clientStreamer = new StreamerMP(this);

	public MojamComponent() {
		String localeString = Options.get(Options.LOCALE, "en");
		setLocale(new Locale(localeString));

		this.setPreferredSize(new Dimension(GAME_WIDTH * SCALE, GAME_HEIGHT
				* SCALE));
		this.setMinimumSize(new Dimension(GAME_WIDTH * SCALE, GAME_HEIGHT
				* SCALE));
		this.setMaximumSize(new Dimension(GAME_WIDTH * SCALE, GAME_HEIGHT
				* SCALE));

		this.addMouseMotionListener(this);
		this.addMouseListener(this);

		TitleMenu menu = new TitleMenu(GAME_WIDTH, GAME_HEIGHT);
		addMenu(menu);
		addKeyListener(this);
		addKeyListener(chat);

		instance = this;
		LevelList.createLevelList();
	}
	
	public void setLocale(Locale locale) {
		MojamComponent.locale = locale;
		MojamComponent.texts = new Texts(locale);
		Locale.setDefault(locale);
	}
	
	public Player getPlayer(short playerId){ return playerMap.get(playerId); }
	public short getUniquePlayerId(){
		short id;
		do {
			id = (short) rand.nextInt(1000);
		} while(playerMap.containsKey(id));
		return id;
	}
	public Player generatePlayer(int team, Keys keys, MouseButtons mouseButtons){
		if(team < 0) team = 1;// TODO: +rand.nextInt(2);
		int x = team == 1 ? level.width * Tile.WIDTH / 2 - 16 : level.width * Tile.WIDTH / 2 - 16;
		int y = team == 1 ? (level.height - 5 - 1) * Tile.HEIGHT - 16 : 7 * Tile.HEIGHT - 16;
		
		Player player = new Player(keys, mouseButtons, x, y,
				team, getUniquePlayerId());
		player.setFacing(team == 1 ? 4 : 0);
		return player;
	}
	public void registerPlayer(Player player){
		playerMap.put(player.id, player);
	}
	
	public boolean isMP(){ return isMultiplayer; }
	
	@Override
	public void mouseDragged(MouseEvent arg0) {
		mouseMoved = true;
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		mouseMoved = true;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
		mouseButtons.releaseAll();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		mouseButtons.setNextState(e.getButton(), true);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		mouseButtons.setNextState(e.getButton(), false);
	}

	@Override
	public void paint(Graphics g) {
	}

	@Override
	public void update(Graphics g) {
	}

	public void start() {
		running = true;
		Thread thread = new Thread(this);
		thread.start();
	}

	public void stop() {
		running = false;
		soundPlayer.stopBackgroundMusic();
		soundPlayer.shutdown();
	}

	private void init() {
		initInput();
		initCharacters();
		
		soundPlayer = new SoundPlayer();		
		soundPlayer.startTitleMusic();

		try {
			emptyCursor = Toolkit.getDefaultToolkit().createCustomCursor(
					new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB),
					new Point(0, 0), "empty");
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		setFocusTraversalKeysEnabled(false);
		requestFocus();

		// hide cursor, since we're drawing our own one
		setCursor(emptyCursor);
	}
	
	private void initInput(){
		inputHandler = new InputHandler(keys);
		addKeyListener(inputHandler);
	}
	
	private void initCharacters(){
		opponentCharacter = Art.HERR_VON_SPECK;
		if(!Options.isCharacterIDset()){
			addMenu(new CharacterSelectionMenu());
		}
		playerCharacter = Options.getCharacterID();
	}

	public void showError(String s) {
		chatWindow.toDraw = 0;
		handleAction(TitleMenu.RETURN_TO_TITLESCREEN);
		addMenu(new GuiError(s));
	}

	private synchronized void createLevel(String levelPath, GameMode mode) {
		LevelInformation li = LevelInformation.getInfoForPath(levelPath);
		
		if (li != null) {
			createLevel(li, mode);
			return;
		} else if (!isMultiplayer) {
			showError("Missing map.");
		}
		showError("Missing map - Multiplayer");
	}

	private synchronized void createLevel(LevelInformation li, GameMode mode) {
		if (!isMultiplayer)
			opponentCharacter = Art.NO_OPPONENT;
		try {
			//level = Level.fromFile(li);
			level = mode.generateLevel(li, playerCharacter, opponentCharacter);
		} catch (Exception ex) {
			ex.printStackTrace();
			showError("Unable to load map.");
			return;
		}
		initLevel();
		paused = false;
	}

	private synchronized void initLevel() {
		if (level == null)
			return;
		//level.init();
		level.addEntity(new Base(34 * Tile.WIDTH, 7 * Tile.WIDTH, Team.Team1));
		if(isServer()){
			player = generatePlayer(1, keys, mouseButtons);
			player.setCanSee(true);
			level.addEntity(player);
			if (isMultiplayer) {
				level.addEntity(new Base(32 * Tile.WIDTH - 20, 32 * Tile.WIDTH - 20, Team.Team2));
			}
		} else {
			for(Entry<Short, Player> e : playerMap.entrySet()){
				System.out.println("Adding from playermap: "+e.getKey());
				level.addEntity(e.getValue());
			}
		}
	}

	@Override
	public void run() {
		long lastTime = System.nanoTime();
		double unprocessed = 0;
		int frames = 0;
		long lastTimer1 = System.currentTimeMillis();

		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		// if (!isMultiplayer) {
		// createLevel();
		// }

		int toTick = 0;

		long lastRenderTime = System.nanoTime();
		int min = 999999999;
		int max = 0;

		while (running) {
			if (!this.hasFocus()) {
				keys.release();
			}

			double nsPerTick = 1000000000.0 / framerate;
			boolean shouldRender = false;
			while (unprocessed >= 1) {
				toTick++;
				unprocessed -= 1;
			}

			int tickCount = toTick;
			if (toTick > 0 && toTick < 3) {
				tickCount = 1;
			}
			if (toTick > 20) {
				toTick = 20;
			}

			for (int i = 0; i < tickCount; i++) {
				toTick--;
				// long before = System.nanoTime();
				tick();
				// long after = System.nanoTime();
				// System.out.println("Tick time took " + (after - before) *
				// 100.0 / nsPerTick + "% of the max time");
				shouldRender = true;
			}
			// shouldRender = true;

			BufferStrategy bs = getBufferStrategy();
			if (bs == null) {
				createBufferStrategy(3);
				continue;
			}

			if (shouldRender) {
				frames++;
				Graphics g = bs.getDrawGraphics();

				Random lastRandom = TurnSynchronizer.synchedRandom;
				TurnSynchronizer.synchedRandom = null;

				render(g);

				TurnSynchronizer.synchedRandom = lastRandom;

				long renderTime = System.nanoTime();
				int timePassed = (int) (renderTime - lastRenderTime);
				if (timePassed < min) {
					min = timePassed;
				}
				if (timePassed > max) {
					max = timePassed;
				}
				lastRenderTime = renderTime;
			}

			long now = System.nanoTime();
			unprocessed += (now - lastTime) / nsPerTick;
			lastTime = now;

			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (shouldRender) {
				if (bs != null) {
					bs.show();
				}
			}

			if (System.currentTimeMillis() - lastTimer1 > 1000) {
				lastTimer1 += 1000;
				fps = frames;
				frames = 0;
			}
		}
	}

	private synchronized void render(Graphics g) {
		if (gameState == 0 && level != null) {
			int xScroll = (int) (player.pos.x - screen.w / 2);
			int yScroll = (int) (player.pos.y - (screen.h - 24) / 2);
			soundPlayer.setListenerPosition((float) player.pos.x,
					(float) player.pos.y);
			level.render(screen, xScroll, yScroll);
		}
		if (!menuStack.isEmpty()) {
			menuStack.peek().render(screen);
		}
		chatWindow.draw(screen);

		if (Options.getAsBoolean(Options.DRAW_FPS, Options.VALUE_FALSE)) {
			Font.defaultFont().draw(screen, texts.FPS(fps), 10, 10);
			if(player != null){
				Font.draw(screen, player.id+" "+player.getTeam()+" "+(int)player.xto+","+(int)player.yto, 100, 10);
			}
		}

		if (player != null && menuStack.size() == 0) {		
		    addHealthBar(screen);
		    addXpBar(screen);
		    addScore(screen);
				
			Font font = Font.defaultFont();
		    if (isMultiplayer) {
		    	font.draw(screen, texts.latency(latencyCacheReady()?""+avgLatency():"-"), 10, 20);
		    }
		}

		if (isMultiplayer && menuStack.isEmpty()) {
			chat.render(screen);
		}

		g.setColor(Color.BLACK);

		g.fillRect(0, 0, getWidth(), getHeight());
		g.translate((getWidth() - GAME_WIDTH * SCALE) / 2,
				(getHeight() - GAME_HEIGHT * SCALE) / 2);
		g.clipRect(0, 0, GAME_WIDTH * SCALE, GAME_HEIGHT * SCALE);

		if (!menuStack.isEmpty() || level != null) {

			// render mouse
			renderMouse(screen, mouseButtons);

			g.drawImage(screen.image, 0, 0, GAME_WIDTH * SCALE, GAME_HEIGHT
					* SCALE, null);
		}

	}

	private void addHealthBar(Screen screen){
	    int maxIndex = Art.panel_healthBar[0].length - 1;
	    int index = maxIndex - Math.round(player.health * maxIndex / player.maxHealth);
	    if (index < 0) index = 0;
        else if (index > maxIndex) index = maxIndex;
        
	    screen.blit(Art.panel_healthBar[0][index], 311, screen.h - 17);
	    screen.blit(Art.panel_heart, 314, screen.h - 24);
	    Font font = Font.defaultFont();
        font.draw(screen, texts.health(player.health, player.maxHealth), 335, screen.h - 21);
	}
	
	private void addXpBar(Screen screen){
	    
	    int xpSinceLastLevelUp = (int)(player.xpSinceLastLevelUp());
	    int xpNeededForNextLevel = (int)(player.nettoXpNeededForLevel(player.plevel+1));

	    int maxIndex = Art.panel_xpBar[0].length - 1;
	    int index = maxIndex - Math.round(xpSinceLastLevelUp * maxIndex / xpNeededForNextLevel);
	    if (index < 0) index = 0;
	    else if (index > maxIndex) index = maxIndex;
	    
	    screen.blit(Art.panel_xpBar[0][index], 311, screen.h - 32);
	    screen.blit(Art.panel_star, 314, screen.h - 40);
	    Font font = Font.defaultFont();
	    font.draw(screen, texts.playerLevel(player.plevel+1), 335, screen.h - 36);
    }
	
	private void addScore(Screen screen){
	    screen.blit(Art.panel_coin, 314, screen.h - 55);
	    Font font = Font.defaultFont();
        font.draw(screen, texts.money(player.score), 335, screen.h - 52);
	}
	
	private void renderMouse(Screen screen, MouseButtons mouseButtons) {

		if (mouseButtons.mouseHidden)
			return;

		int crosshairSize = 15;
		int crosshairSizeHalf = crosshairSize / 2;

		Bitmap marker = new Bitmap(crosshairSize, crosshairSize);

		// horizontal line
		for (int i = 0; i < crosshairSize; i++) {
			if (i >= crosshairSizeHalf - 1 && i <= crosshairSizeHalf + 1)
				continue;

			marker.pixels[crosshairSizeHalf + i * crosshairSize] = 0xffffffff;
			marker.pixels[i + crosshairSizeHalf * crosshairSize] = 0xffffffff;
		}

		screen.blit(marker,
				mouseButtons.getX() / SCALE - crosshairSizeHalf - 2,
				mouseButtons.getY() / SCALE - crosshairSizeHalf - 2);
	}

	private void tick() {
		//Not-In-Focus-Pause
		if (level != null && !isMultiplayer && !paused && !this.isFocusOwner()) {
			keys.release();
			mouseButtons.releaseAll();
			PauseCommand pauseCommand = new PauseCommand(true);
			synchronizer.addCommand(pauseCommand);
			paused = true;
		}

		if (requestToggleFullscreen || keys.fullscreen.wasPressed()) {
		    requestToggleFullscreen = false;
		    setFullscreen(!fullscreen);
		}
		
		if (level != null && level.victoryConditions != null) {
			if(level.victoryConditions.isVictoryConditionAchieved()) {
				int winner = level.victoryConditions.playerVictorious();
				int characterID = winner == MojamComponent.localTeam ? playerCharacter : opponentCharacter;
				addMenu(new WinMenu(GAME_WIDTH, GAME_HEIGHT, winner, characterID));
                level = null;
                return;
            }
        }
		
		chatWindow.tick();
		
		if (packetLink != null) {
			packetLink.tick();
		}

		mouseButtons.setPosition(getMousePosition());
		if (!menuStack.isEmpty()) {
			menuStack.peek().tick(mouseButtons);
		}
		if (mouseMoved) {
			mouseMoved = false;
			mouseHideTime = 0;
			if (mouseButtons.mouseHidden) {
				mouseButtons.mouseHidden = false;
			}
		}
		if (mouseHideTime < 60) {
			mouseHideTime++;
			if (mouseHideTime == 60) {
				mouseButtons.mouseHidden = true;
			}
		}

		clientStreamer.tick();
		
		if(level == null || (menuStack.size() > 0)) {
			mouseButtons.tick();
		} else 
		if (gameState == 0 && level != null) {
			if (/*synchronizer.preTurn() || */true) {
				//synchronizer.postTurn();
				
				mouseButtons.tick();
				/*for (MouseButtons sMouseButtons : synchedMouseButtons) {
					sMouseButtons.tick();
				}*/
				
				if (!paused) {
					if(!chatWindow.isActive){
						/*for (int index = 0; index < keys.getAll().size(); index++) {
							Keys.Key key = keys.getAll().get(index);
							boolean nextState = key.nextState;
							if (key.isDown != nextState) {
								synchronizer.addCommand(new ChangeKeyCommand(index,
										nextState));
							}
						}*/
	
						keys.tick();
						/*for (Keys skeys : synchedKeys) {
							skeys.tick();
						}*/
						
						if(keys.chat.wasPressed()) {
							chatWindow.setActive(true);
						}
					}
					
					if (keys.pause.wasPressed()) {
						keys.release();
						mouseButtons.releaseAll();
						synchronizer.addCommand(new PauseCommand(true));
					}
					
					level.tick();
					if (isMultiplayer) {
						tickChat();
					}
				}

				// every 4 minutes, start new background music :)
				if (System.currentTimeMillis() / 1000 > nextMusicInterval) {
					nextMusicInterval = (System.currentTimeMillis() / 1000) + 4 * 60;
					soundPlayer.startBackgroundMusic();
				}

				if (keys.screenShot.isDown) {
					takeScreenShot();
				}
			}
		}
		// TODO:
		/*long curTime = System.currentTimeMillis();
		if(TurnSynchronizer.synchedRandom != null && packetLink != null && !isServer && curTime - SyncCheckPacket.lastTest > 1000){
			SyncCheckPacket.lastTest = curTime;
			SyncCheckPacket scPacket = new SyncCheckPacket(TurnSynchronizer.synchedRandom.nextLong());
			packetLink.sendPacket(scPacket);
			System.out.println("SENDING PACKET");
		}*/

		if (createServerState == SERVERSTATE_STARTGAME) {
			createServerState = SERVERSTATE_RUNGAME;

			synchronizer.setStarted(true);
			paused = false;
			broadcastPacket(new StartGamePacketCustom(TurnSynchronizer.synchedSeed));
			popMenu();
			gameState = GAMESTATE_INGAME;

		} else if(createServerState == SERVERSTATE_SENDPREGAME){
			/*boolean vanillaServerStart = true;
			if(vanillaServerStart) createServerState = SERVERSTATE_SGVANILLA;
			else {
				createServerState = SERVERSTATE_PREGAME;
				synchronizer = new TurnSynchronizer(MojamComponent.this,
						packetLink, localId, 2);
	
				clearMenus();
				createLevel(TitleMenu.level, TitleMenu.defaultGameMode);
				paused = true;
				
				packetLink.setPacketListener(MojamComponent.this);
				packetLink.sendPacket(new StartPregamePacket(TurnSynchronizer.synchedSeed, level, DifficultyList.getDifficultyID(TitleMenu.difficulty)));
				addMenu(new GuiPregame(this, level));
			}*/
		} else if(createServerState == SERVERSTATE_PREGAME){
			// wait for everyone to ready up
			if(areAllReady()){
				createServerState = SERVERSTATE_STARTGAME;
			}
		} else if(createServerState == SERVERSTATE_SGVANILLA){
			createServerState = SERVERSTATE_RUNGAME;
			synchronizer = new TurnSynchronizer(MojamComponent.this,
					packetLink, localId, 2);

			clearMenus();
			createLevel(TitleMenu.level, TitleMenu.defaultGameMode);
		
			synchronizer.setStarted(true);
			if (TitleMenu.level.vanilla) {
				packetLink.sendPacket(new StartGamePacket(
						TurnSynchronizer.synchedSeed, TitleMenu.level.getUniversalPath(),DifficultyList.getDifficultyID(TitleMenu.difficulty)));
			}
		}
	}

	public boolean areAllReady(){
		for(int i = 0; i < level.players.size(); i++){
			if(!level.players.get(i).isReady) return false;
		}
		return true;
	}
	
	private void tickChat() {
		if (chat.isOpen()) {
			keys.release();
		}

		if (keys.chat.wasReleased()) {
			chat.open();
		}

		chat.tick();

		String msg = chat.getWaitingMessage();
		if (msg != null) {
			synchronizer
			.addCommand(new ChatCommand(texts.playerNameCharacter(playerCharacter) + ": " + msg));
		}
	}

	public static void main(String[] args) {
		MojamComponent mc = new MojamComponent();
		guiFrame = new JFrame(GAME_TITLE);
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(mc);
		guiFrame.setContentPane(panel);
		guiFrame.pack();
		guiFrame.setResizable(false);
		guiFrame.setLocationRelativeTo(null);
		guiFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ArrayList<BufferedImage> icoList = new ArrayList<BufferedImage>();
		icoList.add(Art.icon32);
		icoList.add(Art.icon64);		
		guiFrame.setIconImages(icoList);
		guiFrame.setVisible(true);
		Options.loadProperties();
		setFullscreen(Boolean.parseBoolean(Options.get(Options.FULLSCREEN, Options.VALUE_FALSE)));
		mc.start();
	}

	private static void setFullscreen(boolean fs) {
	    if (fs != fullscreen) {
    		GraphicsDevice device = guiFrame.getGraphicsConfiguration().getDevice();
    		// hide window
    		guiFrame.setVisible(false);
    		guiFrame.dispose();
    		// change options
    		guiFrame.setUndecorated(fs);
    		device.setFullScreenWindow(fs ? guiFrame : null);
    		// display window
    		guiFrame.setLocationRelativeTo(null);
    		guiFrame.setVisible(true);
    		instance.requestFocusInWindow();
    		fullscreen = fs;
	    }
	    Options.set(Options.FULLSCREEN, fullscreen);
	}

	private static volatile boolean requestToggleFullscreen = false;
	public static void toggleFullscreen() {
	    requestToggleFullscreen = true; // only toggle fullscreen in the tick() loop
	}
	
	public static boolean isFullscreen() {
		return fullscreen;
	}

	@Override
	public void handle(int playerId, NetworkCommand packet) {
		//if(isServer) System.out.println("recv:"+playerId+" : "+packet.getClass().getName());
		Player player = getPlayer((short) playerId);
		if (packet instanceof ChangeKeyCommand) {
			ChangeKeyCommand ckc = (ChangeKeyCommand) packet;
			//System.out.println("KEY: "+ckc.getKey()+ckc.getNextState());
			player.keys.getAll().get(ckc.getKey()).nextState = ckc
					.getNextState();
		}
		
		if (packet instanceof ChangeMouseButtonCommand) {
			ChangeMouseButtonCommand ckc = (ChangeMouseButtonCommand) packet;
			player.mouseButtons.nextState[ckc.getButton()] = ckc.getNextState();
		}
		
		if (packet instanceof ChangeMouseCoordinateCommand) {
			ChangeMouseCoordinateCommand ccc = (ChangeMouseCoordinateCommand) packet;
			player.mouseButtons.setPosition(new Point(ccc.getX(), ccc.getY()));
			player.mouseButtons.mouseHidden = ccc.isMouseHidden();
		}
		
		

		if (packet instanceof ChatCommand) {
			ChatCommand cc = (ChatCommand) packet;
			chat.addMessage(cc.getMessage());
		}

		if (packet instanceof PauseCommand) {
			PauseCommand pc = (PauseCommand) packet;
			paused = pc.isPause();
			if (paused) {
				addMenu(new PauseMenu(GAME_WIDTH, GAME_HEIGHT));
			} else {
				popMenu();
			}
		}
	}

	@Override
	public void handle(Packet packet) {
		//if(isServer) System.out.println("PACKET"+(isServer?" (server)":"")+":"+packet.getId());
		if (packet instanceof StartGamePacket) {
			if (!isServer) {
				StartGamePacket sgPacker = (StartGamePacket) packet;
				synchronizer.onStartGamePacket(sgPacker.getGameSeed());
				TitleMenu.difficulty = DifficultyList.getDifficulties().get(sgPacker.getDifficulty());
				createLevel(sgPacker.getLevelFile(), TitleMenu.defaultGameMode);
			}
		} else if (packet instanceof TurnPacket) {
			//synchronizer.onTurnPacket((TurnPacket) packet);
			clientStreamer.onTurnPacket((TurnPacket) packet);
		} else if (packet instanceof StartPregamePacket) {
			if (!isServer) {
				StartPregamePacket sgPacker = (StartPregamePacket) packet;
				localId = sgPacker.id;
				TitleMenu.difficulty = DifficultyList.getDifficulties().get(sgPacker.getDifficulty());
				level = TitleMenu.defaultGameMode.decorateLevel(sgPacker.getLevel());
				initLevel();
				addMenu(new GuiPregame(this, level));
			}
		} else if(packet instanceof StartGamePacketCustom){
			if (!isServer) {
				StartGamePacketCustom sgPacker = (StartGamePacketCustom) packet;
				synchronizer.setStarted(true);
				NetworkPacketLink.SEND_BUFFER_SIZE = NetworkPacketLink.BUFF_SMALL;
				popMenu();
				gameState = GAMESTATE_INGAME;
			}
		} else if(packet instanceof PlayerUpdatePacket){
			if(isServer){
				broadcastPacket(new PlayerUpdatePacket());
			}
		} else if(packet instanceof ChatPacket){
			ChatPacket packer = (ChatPacket) packet;
			chatWindow.addMessage(GuiChatWindow.formatMessage(packer.playerId, packer.message));
		} else if (packet instanceof PingPacket) {
		    PingPacket pp = (PingPacket)packet;
		    synchronizer.onPingPacket(pp);
		    if (pp.getType() == PingPacket.TYPE_ACK) {
		        addToLatencyCache(pp.getLatency());
		    }
		}
		
		else if(packet instanceof HandshakePacket){
			HandshakePacket packer = (HandshakePacket) packet;
			if(packer.getGamename().equals("catacomb_snatch")){
				Client client = new Client(packet.getRecvAddress(), packer.getSendPort(), 0); // TODO: client IDs
				Player player = generatePlayer(-1, new Keys(), new MouseButtons());
				level.addEntity(player);
				player.client = client;
				System.out.println("Handshake from "+client.address+":"+client.port+" assigning "+player.id);
				
				packetLink.sendPacket(new HandshakeResponse(true, (byte) 0).setClient(client));
				packetLink.sendPacket(new SetPlayerPacket(player).setClient(client));
				packetLink.sendPacket(new StartPregamePacket(player.id, level, DifficultyList.getDifficultyID(TitleMenu.difficulty)).setClient(client));
				broadcastPacket(new PlayerUpdatePacket());
			} else {
				System.out.println("BAD HANDSHAKE GAME: "+packer.getGamename());
			}
		} else if(packet instanceof HandshakeResponse){
			HandshakeResponse packer = (HandshakeResponse) packet;
			if(!packer.isAccepted()){
				showError(packer.getReason());
			} else {
				menuStack.clear();
			}
		}
	}
	
	public void broadcastPacket(Packet packet){
		if(level == null) return;
		for(int i = 0 ; i < level.players.size(); i++){
			Player testPlayer = level.players.get(i);
			if(testPlayer.id == player.id) continue;
			if(testPlayer.client == null){
				System.out.println("broadcastPacket() NULL CLIENT: "+testPlayer.id);
				continue;
			}
			System.out.println("broadcastPacket() : "+testPlayer.id);
			packetLink.sendPacket(packet, testPlayer.client.address, testPlayer.client.port);
		}
	}
	
    private void addToLatencyCache(int latency) {
        if (nextLatencyCacheIdx >= latencyCache.length) nextLatencyCacheIdx=0;
        if (latencyCacheState != CACHE_PRIMED) {
            if (nextLatencyCacheIdx == 0 && latencyCacheState == CACHE_PRIMING) latencyCacheState = CACHE_PRIMED;
            if (latencyCacheState == CACHE_EMPTY) latencyCacheState = CACHE_PRIMING;
        }
        latencyCache[nextLatencyCacheIdx++] = latency;
    }

    private boolean latencyCacheReady() { return latencyCacheState == CACHE_PRIMED; }
    private int avgLatency() {
        int total = 0;
        for (int latency : latencyCache) { total += latency; }
        return total / latencyCache.length; // rounds down
    }
	
	@Override
	public void buttonPressed(ClickableComponent component) {
		if (component instanceof Button) {
			final Button button = (Button) component;
			handleAction(button.getId());
		}
	}

	public void handleAction(int id) {
		switch (id) {
			case TitleMenu.RETURN_TO_TITLESCREEN:
				clearMenus();
				level = null;
				TitleMenu menu = new TitleMenu(GAME_WIDTH, GAME_HEIGHT);
				addMenu(menu);
				break;
				
			case TitleMenu.START_GAME_ID:
				clearMenus();
				isMultiplayer = false;
				chat.clear();

				localId = 0;
				MojamComponent.localTeam = Team.Team1;
				synchronizer = new TurnSynchronizer(this, null, 0, 1);
				synchronizer.setStarted(true);

			localId = 0;
			synchronizer = new TurnSynchronizer(this, null, 0, 1);
			synchronizer.setStarted(true);

			createLevel(TitleMenu.level, TitleMenu.defaultGameMode);
			soundPlayer.stopBackgroundMusic();
			gameState = GAMESTATE_INGAME;
		} else if (id == TitleMenu.SELECT_LEVEL_ID) {
			addMenu(new LevelSelect(false));
		} else if (id == TitleMenu.SELECT_HOST_LEVEL_ID) {
			addMenu(new LevelSelect(true));
		} else if (id == TitleMenu.UPDATE_LEVELS) {
			GuiMenu menu = menuStack.pop();
			if (menu instanceof LevelSelect) {
				addMenu(new LevelSelect(((LevelSelect) menu).bHosting));
			} else {
				addMenu(new LevelSelect(false));
			}
		} else if (id == TitleMenu.HOST_GAME_ID) {
			//addMenu(new HostingWaitMenu());
			synchronizer = new TurnSynchronizer(this, packetLink, localId, 2);

			clearMenus();
			System.out.println("Starting server...");
			createLevel(TitleMenu.level, TitleMenu.defaultGameMode);
			
			addMenu(new GuiPregame(this, level));
			// activate network doodads
			isMultiplayer = true;
			isServer = true;
			
			try {
				if(packetLink != null){
					packetLink.close();
				}
				packetLink = new NetworkPacketLink(NetworkPacketLink.SERVER_PORT);
				try {
					((NetworkPacketLink) packetLink).startWrite("localhost");
				} catch (Exception e) {
					e.printStackTrace();
				}
				packetLink.setPacketListener(MojamComponent.this);
				createServerState = SERVERSTATE_PREGAME;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (id == TitleMenu.JOIN_GAME_ID) {
			addMenu(new JoinGameMenu());
		} else if (id == TitleMenu.CANCEL_JOIN_ID) {
			popMenu();
			if (hostThread != null) {
				hostThread.interrupt();
				hostThread = null;
			}
		} else if (id == TitleMenu.PERFORM_JOIN_ID) {
			isMultiplayer = true;
			isServer = false;
			NetworkPacketLink.SEND_BUFFER_SIZE = NetworkPacketLink.BUFF_BIG;
			
			try {
				localId = 1;
				packetLink = new ClientSidePacketLink(TitleMenu.ip, 0);
				synchronizer = new TurnSynchronizer(this, packetLink, localId,2);
				packetLink.setPacketListener(this);
				packetLink.sendPacket(new HandshakePacket());
			} catch (Exception e) {
				e.printStackTrace();
				// System.exit(1);
				menuStack.clear();
				addMenu(new TitleMenu(GAME_WIDTH, GAME_HEIGHT));
			}
		} else if (id == TitleMenu.HOW_TO_PLAY) {
			addMenu(new HowToPlay());
		} else if (id == TitleMenu.OPTIONS_ID) {
			addMenu(new OptionsMenu());
		} else if (id == TitleMenu.SELECT_DIFFICULTY_ID) {
			addMenu(new DifficultySelect(false));
		} else if (id == TitleMenu.SELECT_DIFFICULTY_HOSTING_ID) {
			addMenu(new DifficultySelect(true));
		} else if (id == TitleMenu.KEY_BINDINGS_ID) {
			addMenu(new KeyBindingsMenu(keys, inputHandler));
		} else if (id == TitleMenu.EXIT_GAME_ID) {
			System.exit(0);
		} else if (id == TitleMenu.RETURN_ID) {
			synchronizer.addCommand(new PauseCommand(false));
			keys.tick();
		} else if (id == TitleMenu.BACK_ID) {
			popMenu();
		} else if(id == TitleMenu.SEND_READY){
			System.out.println("Local ready : "+localId);
			boolean flag = !player.isReady;
			player.isReady = flag;
			Packet packet = new PlayerUpdatePacket();
			if(isServer) broadcastPacket(packet);
			else packetLink.sendPacket(packet);
		}
	}
	
	private void clearMenus() {
		while (!menuStack.isEmpty()) {
			menuStack.pop();
		}
	}

	private void addMenu(GuiMenu menu) {
		menuStack.add(menu);
		menu.addButtonListener(this);
	}

	private void popMenu() {
		if (!menuStack.isEmpty()) {
			menuStack.pop();
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if(chatWindow.isActive()){
			chatWindow.onKeyPress(e);
		} else if (!menuStack.isEmpty()) {
			menuStack.peek().keyPressed(e);
		}
		
		if(e.getKeyCode() == KeyEvent.VK_F4){
			chatWindow.addMessage("SYS: "+level.entities.size()+" entities");
			for(Entity en : level.entities){
				if(en instanceof Player) continue;
				en.remove();
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (!menuStack.isEmpty()) {
			menuStack.peek().keyReleased(e);
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		if (!menuStack.isEmpty()) {
			menuStack.peek().keyTyped(e);
		}
	}

	public static File getMojamDir() {
		if (mojamDir == null) {
			mojamDir = getAppDir("mojam");
		}
		return mojamDir;
	}

	public static EnumOS2 getOs() {
		String s = System.getProperty("os.name").toLowerCase();
		if (s.contains("win")) {
			return EnumOS2.windows;
		}
		if (s.contains("mac")) {
			return EnumOS2.macos;
		}
		if (s.contains("solaris")) {
			return EnumOS2.solaris;
		}
		if (s.contains("sunos")) {
			return EnumOS2.solaris;
		}
		if (s.contains("linux")) {
			return EnumOS2.linux;
		}
		if (s.contains("unix")) {
			return EnumOS2.linux;
		} else {
			return EnumOS2.unknown;
		}
	}

	public static File getAppDir(String s) {
		String s1 = System.getProperty("user.home", ".");
		File file;
		switch (EnumOSMappingHelper.enumOSMappingArray[getOs().ordinal()]) {
		case 1: // '\001'
		case 2: // '\002'
			file = new File(s1, (new StringBuilder()).append('.').append(s)
					.append('/').toString());
			break;

		case 3: // '\003'
			String s2 = System.getenv("APPDATA");
			if (s2 != null) {
				file = new File(s2, (new StringBuilder()).append(".").append(s)
						.append('/').toString());
			} else {
				file = new File(s1, (new StringBuilder()).append('.').append(s)
						.append('/').toString());
			}
			break;

		case 4: // '\004'
			file = new File(s1, (new StringBuilder())
					.append("Library/Application Support/").append(s)
					.toString());
			break;

		default:
			file = new File(s1, (new StringBuilder()).append(s).append('/')
					.toString());
			break;
		}
		if (!file.exists() && !file.mkdirs()) {
			throw new RuntimeException((new StringBuilder())
					.append("The working directory could not be created: ")
					.append(file).toString());
		} else {
			return file;
		}
	}

	public void takeScreenShot() {
		BufferedImage screencapture;

		try {
			screencapture = new Robot().createScreenCapture(guiFrame
					.getBounds());

			File file = new File(getMojamDir()+"/"+"screenShot" + sShotCounter++ + ".png");
			while(file.exists()) {
			    file = new File(getMojamDir()+"/"+"screenShot" + sShotCounter++ + ".png");
			}
			
			ImageIO.write(screencapture, "png", file);
		} catch (AWTException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isPaused(){
		return paused;
	}

	public int getLocalId() { return localId; }
	public void setLocalId(int id) { localId = id; }
	public static String cleanClassName(String s){
		int i = s.lastIndexOf(".");
		if(i >= 0) {
			return s.substring(i);
		}
		return s;
	}
	
	public boolean isServer(){
		return !isMP() || isServer;
	}
}