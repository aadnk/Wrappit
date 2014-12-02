package com.comphenix.wrappit;

import java.io.File;
import java.util.Arrays;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.utility.Constants;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.wrappit.io.IOUtil;
import com.comphenix.wrappit.minecraft.CodePacketReader;
import com.comphenix.wrappit.wiki.WikiPacketReader;
import com.google.common.base.CaseFormat;

public class Wrappit {

	public static void main(String[] args) {
		try {
			new Wrappit(args);
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	private Wrappit(String[] args) throws Throwable {
		if (args.length == 0) {
			System.err.println("You must specify the wiki page location!");
			return;
		}

		// Initialize ProtocolLib
		MinecraftReflection.setMinecraftPackage(Constants.NMS, Constants.OBC);

		CodePacketReader codeReader = new CodePacketReader();
		WikiPacketReader wikiReader = new WikiPacketReader(new File(args[0]));
		WrapperGenerator generator = new WrapperGenerator(codeReader, wikiReader);

		File folder = new File("Packets");
		if (folder.exists())
			folder.delete();
		folder.mkdirs();

		System.out.println("Generating wrappers...");

		for (PacketType type : PacketType.values()) {
			try {
				System.out.println("Generating wrapper for " + type.name());
				String className = "Wrapper" + getCamelCase(type.getProtocol()) + getCamelCase(type.getSender()) + getCamelCase(type.name());
				File file = new File(folder, className + ".java");
				file.createNewFile();
				IOUtil.writeLines(file, Arrays.asList(generator.generateClass(type)));
			} catch (Throwable ex) {
				System.err.println("Failed to generate wrapper for " + type.name());
				ex.printStackTrace();
			}
		}

		System.out.println("Done!");
	}

	public static String getCamelCase(Enum<?> enumValue) {
		return getCamelCase(enumValue.name());
	}
	
	public static String getCamelCase(String text) {
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, text);
	}
}
