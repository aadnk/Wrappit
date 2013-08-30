package com.comphenix.wrappit;

import java.io.File;
import java.io.IOException;
import com.comphenix.wrappit.minecraft.CodePacketReader;
import com.comphenix.wrappit.wiki.WikiPacketReader;

public class Application {

	public static void main(String[] args) throws IOException {
		CodePacketReader codeReader = new CodePacketReader();
		WikiPacketReader wikiReader = new WikiPacketReader(new File("I:/Temp/MinecraftProtocol/Protocol.htm"));
		WrapperGenerator generator = new WrapperGenerator(codeReader, wikiReader);
		
		System.out.println(generator.generateClass(0x3F));
	}
}
