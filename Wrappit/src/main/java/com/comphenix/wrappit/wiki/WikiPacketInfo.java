package com.comphenix.wrappit.wiki;

import java.util.List;

import com.comphenix.protocol.PacketType;

public class WikiPacketInfo {
	private PacketType type;
	private List<WikiPacketField> packetFields;
	
	public WikiPacketInfo(PacketType type, List<WikiPacketField> packetFields) {
		this.type = type;
		this.packetFields = packetFields;
	}

	public PacketType getType() {
		return type;
	}
	
	public Iterable<WikiPacketField> getPacketFields() {
		return packetFields;
	}
	
	@Override
	public String toString() {
		return String.valueOf(type);
	}
}
