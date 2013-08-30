package com.comphenix.wrappit.wiki;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Retrieve valuable information from the Minecraft Protocol Wiki.
 * 
 * @author Kristian
 */
public class WikiPacketReader {
	public static final String STANDARD_URL = "http://www.wiki.vg/Protocol";
	
	/**
	 * Parse a given packet header.
	 */
	private static final String PARSE_HEADER = "((?:[a-zA-Z_/-]* )+)\\(0x([0-9A-F]+)\\)\\s*";
	
	// Stored packet information
	private Map<Integer, WikiPacketInfo> packets;
	
	// Used to parse packets
	private Pattern headerParser = Pattern.compile(PARSE_HEADER, Pattern.CASE_INSENSITIVE);
	
	public WikiPacketReader() throws IOException {
		this(STANDARD_URL);
	}
	
	public WikiPacketReader(String url) throws IOException {
		packets = loadFromDocument(Jsoup.connect(url).get());
	}
	
	public WikiPacketReader(File file) throws IOException {
		packets = loadFromDocument(Jsoup.parse(file, null));
	}
	
	/**
	 * Retrieve a column element in a set of rows.
	 * @param rows - the set of rows to look in.
	 * @param row - the row index.
	 * @param column - the column index.
	 * @return The cell, or NULL if not found.
	 */
	private Element getCell(Elements rows, int row, int column) {
		if (row < rows.size()) {
			Elements columns = rows.get(row).select("td, th");
			
			if (column < columns.size())
				return columns.get(column);
		}
		
		return null;
	}
	
	private String[] getCells(Element row, int start, int count) {
		String[] result = new String[count];
		Elements columns = row.getElementsByTag("td");
		
		// Convert each cell to text
		for (int i = 0; i < count; i++) {
			// We'll ignore non-existant columns
			if (i + start < columns.size()) {
				result[i] = columns.get(i + start).text();
			}
		}
		return result;
	}
	
	private Element getPreviousHeader(Element table) {
		Element current = table;
		
		while (current != null && !current.tagName().equals("h3")) {
			current = current.previousElementSibling();
		}
		return current;
	}
	
	private WikiPacketInfo parseInfo(Element previousHeader) {
		if (previousHeader == null) 
			return null;
		Matcher matcher = headerParser.matcher(previousHeader.text());
		
		if (matcher.matches()) {
			return new WikiPacketInfo(Integer.parseInt(matcher.group(2), 16), matcher.group(1).trim(), null);
		} else {
			// Inform the user
			throw new IllegalStateException("Cannot parse header '" + previousHeader.text() + "'");
		}
	}
	
	private Map<Integer, WikiPacketInfo> loadFromDocument(Document doc) {
		Map<Integer, WikiPacketInfo> result = new HashMap<Integer, WikiPacketInfo>();
		
		for (Element tbody : doc.select("table.wikitable > tbody")) {
			Elements rows = tbody.getElementsByTag("tr");
			
			// Make sure we are dealing with some kind of packet table
			Element firstHeader = getCell(rows, 0, 0);
			List<WikiPacketField> fields = new ArrayList<WikiPacketField>();

			if (firstHeader != null && firstHeader.text().contains("Packet")) {
				// Add the packet
				Element previousHeader = getPreviousHeader(tbody.parent());
				WikiPacketInfo info = parseInfo(previousHeader);
				
				// Skip the first and last row
				for (int i = 1; i < rows.size() - 1; i++) {
					String[] data = getCells(rows.get(i), i == 1 ? 1 : 0, 4);
					fields.add(new WikiPacketField(data[0], data[1], data[2], data[3]));
				}
				
				// Save this
				result.put(info.getPacketID(), 
						new WikiPacketInfo(info.getPacketID(), info.getPacketName(), fields));
			}
		}
		
		return result;
	}

	public Collection<WikiPacketInfo> getCachedPackets() {
		return packets.values();
	}

	/**
	 * Attempt to retrieve information about a packet from its ID.
	 * @param packetID - the packet to retrieve.
	 * @return Information about this packet.
	 * @throws IOException If this packet cannot be found on the Wiki.
	 */
	public WikiPacketInfo readPacket(int packetID) throws IOException {
		WikiPacketInfo result = packets.get(packetID);
		
		if (result != null)
			return result;
		else
			throw new IOException("Packet " + packetID + " cannot be found on the wiki.");
	}
}
