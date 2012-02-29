package com.mojang.mojam.entity;

import java.util.ArrayList;
import java.util.HashMap;

import com.mojang.mojam.MojamComponent;
import com.mojang.mojam.entity.loot.Loot;
import com.mojang.mojam.entity.mob.*;

public class EntityList {
	
	public static HashMap<Short, Entity> idToEntityMap = new HashMap<Short, Entity>();  

	public static ArrayList<Short> mpTransfer = new ArrayList<Short>();
	private static HashMap<Short, Class<? extends Entity>> shortToEntityMap = new HashMap<Short, Class<? extends Entity>>();
	private static HashMap<Class<? extends Entity>, Short> tileToShortMap = new HashMap<Class<? extends Entity>, Short>();
	
	static {
		//registerEntity((short) 1, Player.class);
		registerEntity((short) 11, Bat.class);
		//registerEntity((short) 12, Mummy.class);
		//registerEntity((short) 13, RailDroid.class);
		//registerEntity((short) 14, Scarab.class);
		registerEntity((short) 15, Snake.class);

		//registerEntity((short) 30, Loot.class);
	}
	
	public static void registerEntity(short id, Class<? extends Entity> mobclass){
		shortToEntityMap.put(id, mobclass);
		tileToShortMap.put(mobclass, id);
		mpTransfer.add(id);
	}
	
	public static short classToShort(Class<? extends Entity> entityclass){
		if(!tileToShortMap.containsKey(entityclass)) {
			if(!MojamComponent.instance.isServer()) System.out.println("EntityList ... Unknown class: "
					+MojamComponent.cleanClassName(entityclass.getName()));
			return 0;
		}
		return tileToShortMap.get(entityclass);
	}
	
	public static Entity shortToEntity(short i, double xPos, double yPos){
		return classToEntity(shortToEntityMap.get(i), xPos, yPos);
	}
	
	public static Entity classToEntity(Class<? extends Entity> mobclass, double xPos, double yPos){
		Entity entity = null;
		try
        {
            if (mobclass != null)
            {
                entity = (Entity) mobclass.getConstructor(new Class[]{
                		Double.TYPE, Double.TYPE
                }).newInstance(new Object[]{
                		xPos, yPos
                });
            	/*if(mobclass == Bat.class){
            		entity = new Bat(posX, posY);
            	}*/
            }
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        return entity;
	}
}
