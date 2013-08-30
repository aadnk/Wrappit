package com.comphenix.wrappit.minecraft;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.server.Packet;

/**
 * Gives access to the internal Minecraft packet registry.
 * 
 * @author Kristian
 */
public class PacketRegistry {
	private Map<Class<?>, Integer> classToId;
	private Map<Integer, Class<?>> idToClass;
	
	@SuppressWarnings("unchecked")
	private void initializeMaps() {
		if (classToId == null) {
			idToClass = new HashMap<Integer, Class<?>>();
			
			for (Field candidate : Packet.class.getDeclaredFields()) {
				if (Map.class.isAssignableFrom(candidate.getType())) {
					candidate.setAccessible(true);
					
					try {
						classToId = (Map<Class<?>, Integer>) candidate.get(null);
					} catch (Exception e) {
						throw new RuntimeException("Cannot read packet registry.");
					}
				}
			}

			// Linear scan to reverse key and value
			for (Entry<Class<?>, Integer> entry : classToId.entrySet()) {
				idToClass.put(entry.getValue(), entry.getKey());
			}
		}
	}
	
	/**
	 * Get the packet ID associated with a given class.
	 * @param packetClass - the packet class to look up.
	 * @return The packet ID, or NULL if this is not a valid packet class.
	 */
	public Integer getPacketID(Class<?> packetClass) {
		initializeMaps();
		return classToId.get(packetClass);
	}
	
	/**
	 * Get the packet class associated with a given ID.
	 * @param packetID - the given packet ID.
	 * @return A packet class, or NULL if no such packet has been registered.
	 */
	public Class<?> getPacketClass(int packetID) {
		initializeMaps();
		return idToClass.get(packetID);
	}
}
