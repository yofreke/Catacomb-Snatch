package com.mojang.mojam.gui;

import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.MouseButtons;
import com.mojang.mojam.entity.Player;
import com.mojang.mojam.level.Level;
import com.mojang.mojam.level.LevelInformation;
import com.mojang.mojam.resources.MD5Checksum;
import com.mojang.mojam.screen.Art;
import com.mojang.mojam.screen.Bitmap;
import com.mojang.mojam.screen.Screen;

public class GuiPregame extends GuiMenu {

	public MojamComponent component;
	public Level level;
	private Bitmap minimap;
	private boolean mapExists;
	
	public GuiPregame(MojamComponent mc, Level level){
		this.component = mc;
		this.level = level;
		
		addButton(new Button(TitleMenu.RETURN_TO_TITLESCREEN, "Cancel", 48, 330));
		addButton(new Button(TitleMenu.SEND_READY, "Ready", 184, 330));
		
		minimap = level.getInfo().getButtonMinimap();
		mapExists = doesMapExist(level);
		if(!mapExists){
			performSave(level);
		}
	}
	
	public boolean doesMapExist(Level level){
		if(level.getInfo().vanilla) return true;
		
		System.out.println("TEST1:"+level.getInfo().getPath(false));
		File file = new File(level.getInfo().getPath(false));
		if(file.exists()){
			System.out.println("TEST2:"+MD5Checksum.getMD5Checksum(file.getPath()));
			System.out.println("     :"+level.getInfo().getChecksum());
			String cs = MD5Checksum.getMD5Checksum(file.getPath());
			if(cs.equals(level.getInfo().getChecksum())){
				System.out.println("  Match!");
				return true;
			}
		}
		return false;
	}
	
	public void performSave(Level level_){
		System.out.println("Saving local map: "+level_.getInfo().getNameRaw());
		BufferedImage output = level_.createMapImage();
		
		String initialPath = level_.getInfo().getPath(false);
		int i = 0;
		int j = initialPath.lastIndexOf(".");
		String path = initialPath.substring(0,j);
		File outputFile = new File(path+".png");
		while(outputFile.exists()) {
			if(i++ > 100) {
				component.showError("Map save error");
				return;
			}
			outputFile = new File(path+"_"+i+".png");
		};
		outputFile.mkdirs();
			
		System.out.println("  path:"+outputFile.getPath());
	    try {
			ImageIO.write(output, "png", outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		component.handleAction(TitleMenu.BACK_ID);
	}
	
	public void render(Screen screen) {
		screen.clear(0);
		screen.blit(Art.emptyBackground, 0, 0);
		int leftMargin = 48;
		Font.defaultFont().draw(screen, "Server Map", leftMargin/2, 24);
		Font.defaultFont().draw(screen, "Name:", leftMargin, 40);
		Font.defaultFont().draw(screen, "Author:", leftMargin, 50);
		Font.defaultFont().draw(screen, "Desc:", leftMargin, 60);
		Font.defaultFont().draw(screen, "Players", leftMargin/2, 155);
		for(int i = 0; i < component.level.players.size(); i++){
			Player player = component.level.players.get(i);
			Font f = player.isReady ? Font.defaultFont() : Font.FONT_RED;
			f.draw(screen, (player.id==component.player.id?"-":"")+player.name+" "+player.getTeam(), leftMargin, 165+i*10);
		}
		LevelInformation li = level.getInfo();
		Font.FONT_BLUE.draw(screen, li.getName(), 120, 40);
		Font.FONT_BLUE.draw(screen, li.levelAuthor, 120, 50);
		Font.FONT_BLUE.draw(screen, li.levelDescription, 120, 60);
		String s = li.getPath(false);
		Font.FONT_GRAY.draw(screen, s.substring(s.lastIndexOf("level")), 130, 135);
		if(minimap != null){
			screen.blit(minimap, leftMargin, 80);
		}
		super.render(screen);
	}
	
	
	@Override
	public void buttonPressed(ClickableComponent button) {
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
	}

	@Override
	public void keyTyped(KeyEvent arg0) {
	}

}
