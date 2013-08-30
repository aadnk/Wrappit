package com.comphenix.wrappit.wiki;

import java.util.List;

public class WikiPacketInfo {
	private int packetID;
	private String packetName;
	private List<WikiPacketField> packetFields;
	
	public WikiPacketInfo(int packetID, String packetName, List<WikiPacketField> packetFields) {
		this.packetID = packetID;
		this.packetName = packetName;
		this.packetFields = packetFields;
	}

	public int getPacketID() {
		return packetID;
	}
	
	public String getPacketName() {
		return packetName;
	}
	
	public Iterable<WikiPacketField> getPacketFields() {
		return packetFields;
	}
	
	@Override
	public String toString() {
		return String.format("%s (%s)", packetName, packetID);
	}
}
