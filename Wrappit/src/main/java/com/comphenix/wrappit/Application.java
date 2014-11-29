package com.comphenix.wrappit;

import java.io.File;
import java.io.IOException;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.wrappit.minecraft.CodePacketReader;
import com.comphenix.wrappit.wiki.WikiPacketReader;

public class Application {

	public static void main(String[] args) throws IOException {
		// Initialize ProtocolLib
		MinecraftReflection.setMinecraftPackage("net.minecraft.server.v1_7_R1", "org.bukkit.craftbukkit.v1_7_R1");
		
		CodePacketReader codeReader = new CodePacketReader();
		WikiPacketReader wikiReader = new WikiPacketReader(new File("I:/Temp/MinecraftProtocol/1.7.2/Protocol - MinecraftCoalition.htm"));
		WrapperGenerator generator = new WrapperGenerator(codeReader, wikiReader);

		
		System.out.println(generator.generateClass(PacketType.Play.Client.WINDOW_CLICK));
	}
}
