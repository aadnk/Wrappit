package com.comphenix.wrappit.wiki;

public class WikiPacketField {
	private String fieldName;
	private String fieldType;
	private String notes;
	
	public WikiPacketField(String fieldName, String fieldType, String notes) {
		this.fieldName = fieldName;
		this.fieldType = fieldType;
		this.notes = notes;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
	public String getFieldType() {
		return fieldType;
	}
	
	public String getNotes() {
		return notes != null ? notes : "";
	}
}