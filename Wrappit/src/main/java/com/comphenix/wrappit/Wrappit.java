package com.comphenix.wrappit;

import java.io.File;
import java.util.Arrays;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.utility.Constants;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.wrappit.io.IOUtil;
import com.comphenix.wrappit.minecraft.CodePacketReader;
import com.comphenix.wrappit.test.WrapperTest;
import com.comphenix.wrappit.wiki.WikiPacketReader;
import com.google.common.base.CaseFormat;

public class Wrappit {
	private static File wikiPage = null;
	private static File packetWrapper = null;
	private static boolean test = false;

	public static void main(String[] args) {
		/*OptionParser parser = new OptionParser() {{
			accepts("wikiPage").withRequiredArg().ofType(File.class);
			accepts("packetWrapper").withOptionalArg().ofType(File.class);
			accepts("flagOnly");
		}};

		OptionSet options = parser.parse(args);
		wikiPage = (File) options.valueOf("wikiPage");
		if (test = options.has("test")) {
			if (options.has("packets")) {
				packetWrapper = (File) options.valueOf("packetWrapper");
				if (! packetWrapper.getName().endsWith(".jar")) {
					System.err.println("PacketWrapper must be a jar file!");
					System.exit(2);
				}
			} else {
				System.err.println("Must specify PacketWrapper location!");
				System.exit(2);
			}
		}*/
		test = true;
		packetWrapper = new File("C:/Users/Dan/Desktop/Eclipse/eclipse/PacketWrapper/PacketWrapper/target/PacketWrapper.jar");

		try {
			new Wrappit();
		} catch (Throwable ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private Wrappit() throws Throwable {
		// Special case if we're only testing
		if (test) {
			WrapperTest.test(packetWrapper);
			return;
		}

		// Initialize ProtocolLib
		MinecraftReflection.setMinecraftPackage(Constants.NMS, Constants.OBC);

		CodePacketReader codeReader = new CodePacketReader();
		WikiPacketReader wikiReader = new WikiPacketReader(wikiPage);
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
