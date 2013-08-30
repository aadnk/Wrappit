package com.comphenix.wrappit.minecraft;

import java.lang.reflect.Field;
import java.util.List;

public class CodePacketInfo {
	private final List<Field> memoryOrder;
	private final List<Field> networkOrder;
	private final int packetID;
	
	public CodePacketInfo(List<Field> memoryOrder, List<Field> networkOrder, int packetID) {
		this.memoryOrder = memoryOrder;
		this.networkOrder = networkOrder;
		this.packetID = packetID;
	}

	/**
	 * Determine if the memory and network contain the same number of fields.
	 * <p>
	 * If not, we may have to do some manual work.
	 * @return TRUE if the field count is the same, FALSE otherwise.
	 */
	public boolean isBalanced() {
		return memoryOrder.size() == networkOrder.size();
	}
	
	public List<Field> getMemoryOrder() {
		return memoryOrder;
	}

	public List<Field> getNetworkOrder() {
		return networkOrder;
	}

	public int getPacketID() {
		return packetID;
	}
}
