package com.comphenix.wrappit;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.v1_8_R1.Block;
import net.minecraft.server.v1_8_R1.BlockPosition;
import net.minecraft.server.v1_8_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R1.DataWatcher;
import net.minecraft.server.v1_8_R1.IChatBaseComponent;
import net.minecraft.server.v1_8_R1.ItemStack;
import net.minecraft.server.v1_8_R1.NBTTagCompound;
import net.minecraft.server.v1_8_R1.ServerPing;
import net.minecraft.server.v1_8_R1.Vec3D;
import net.minecraft.server.v1_8_R1.WorldType;

import com.comphenix.protocol.PacketType;
import com.comphenix.wrappit.minecraft.CodePacketInfo;
import com.comphenix.wrappit.minecraft.CodePacketReader;
import com.comphenix.wrappit.utils.CaseFormating;
import com.comphenix.wrappit.utils.IndentBuilder;
import com.comphenix.wrappit.wiki.WikiPacketField;
import com.comphenix.wrappit.wiki.WikiPacketInfo;
import com.comphenix.wrappit.wiki.WikiPacketReader;
import com.mojang.authlib.GameProfile;

public class WrapperGenerator {
	public enum Modifiers {
		BLOCK(Block.class,                            "Material",               "getBlocks()"),
		BLOCK_POSITION(BlockPosition.class,           "BlockPosition",          "getBlockPositionModifier()"),
		BOOLEANS(boolean.class,                       "boolean",                "getSpecificModifier(boolean.class)"),
		BYTE_ARRAYS(byte[].class,                     "byte[]",                 "getByteArrays()"),
		BYTES(byte.class,                             "byte",                   "getBytes()"),
		CHAT_BASE_COMPONENT(IChatBaseComponent.class, "WrappedChatComponent",   "getChatComponents()"),
		CHUNK_COORD_INT_PAIR(ChunkCoordIntPair.class, "ChunkCoordIntPair",      "getChunkCoordIntPairs()"),
		COMPONENT_ARRAY(IChatBaseComponent[].class,   "WrappedChatComponent[]", "getChatComponentArrays()"),
		DATA_WATCHER_MODIFIER(DataWatcher.class,      "WrappedDataWatcher",     "getDataWatcherModifier()"),
		DOUBLES(double.class,                         "double",                 "getDoubles()"),
		ENUMS(Enum.class,                             "Enum<?>",                "getSpecificModifier(Enum.class)"),
		FLOATS(float.class,                           "float",                  "getFloat()"),
		GAME_PROFILE(GameProfile.class,               "WrappedGameProfile",     "getGameProfiles()"),
		INTEGER_ARRAYS(int[].class,                   "int[]",                  "getIntegerArrays()"),
		INTEGERS(int.class,                           "int",                    "getIntegers()"),
		ITEM_ARRAY_MODIFIER(ItemStack[].class,        "ItemStack[]",            "getItemArrayModifier()"),
		ITEM_MODIFIER(ItemStack.class,                "ItemStack",              "getItemModifier()"),
		LONGS(long.class,                             "long",                   "getLongs()"),
		MAP(Map.class,                                "Map<?,?>",               "getSpecificModifier(Map.class)"),
		NBT_MODIFIER(NBTTagCompound.class,            "NbtBase<?>",             "getNbtModifier()"),
		POSITION_LIST(List.class,                     "List<BlockPosition>",    "getBlockPositionCollectionModifier()"),
		SET(Set.class,                                "Set<?>",                 "getSpecificModifier(Set.class)"),
		PUBLIC_KEY_MODIFIER(PublicKey.class,          "PublicKey",              "getSpecificModifier(PublicKey.class)"),
		SERVER_PING(ServerPing.class,                 "WrappedServerPing",      "getServerPings()"),
		SHORTS(short.class,                           "short",                  "getShorts()"),
		STRING_ARRAYS(String[].class,                 "String[]",               "getStringArrays()"),
		STRINGS(String.class,                         "String",                 "getStrings()"),
		UUID(UUID.class,                              "UUID",                   "getSpecificModifier(UUID.class)"),
		VEC3D(Vec3D.class,                            "Vector",                 "getVectors()"),
		WORLD_TYPE_MODIFIER(WorldType.class,          "WorldType",              "getWorldTypeModifier()");

		private static Map<Class<?>, Modifiers> inputLookup;

		static {
			inputLookup = new HashMap<Class<?>, Modifiers>();

			for (Modifiers modifier : values()) {
				inputLookup.put(modifier.inputType, modifier);
			}
		}

		public static Modifiers getByInputType(Class<?> inputType) {
			for (; inputType != null && !inputType.equals(Object.class); inputType = inputType.getSuperclass()) {
				Modifiers mod = inputLookup.get(inputType);

				if (mod != null)
					return mod;
			}

			// Unable to find modifier
			return null;
		}

		private Class<?> inputType;
		private String outputType;
		private String name;

		private Modifiers(Class<?> inputType, String outputType, String name) {
			this.inputType = inputType;
			this.outputType = outputType;
			this.name = name;
		}

		public Class<?> getInputType() {
			return inputType;
		}

		public String getMethodName() {
			return name;
		}

		public String getOutputType() {
			return outputType;
		}

		public boolean isWrapper() {
			switch (this) {
			case BLOCK:
			case BLOCK_POSITION:
			case CHAT_BASE_COMPONENT:
			case CHUNK_COORD_INT_PAIR:
			case COMPONENT_ARRAY:
			case DATA_WATCHER_MODIFIER:
			case GAME_PROFILE:
			case POSITION_LIST:
			case SERVER_PING:
				return true;
			default:
				return false;
			}
		}
	}

	private static final String NEWLN = System.getProperty("line.separator");

	private static final String[] HEADER = {
		"/**",
		" * This file is part of PacketWrapper.",
		" * Copyright (C) 2012-2015 Kristian S. Strangeland",
		" * Copyright (C) 2015 dmulloy2",
		" *",
		" * PacketWrapper is free software: you can redistribute it and/or modify",
		" * it under the terms of the GNU Lesser General Public License as published by",
		" * the Free Software Foundation, either version 3 of the License, or",
		" * (at your option) any later version.",
		" *",
		" * PacketWrapper is distributed in the hope that it will be useful,",
		" * but WITHOUT ANY WARRANTY; without even the implied warranty of",
		" * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the",
		" * GNU General Public License for more details.",
		" *",
		" * You should have received a copy of the GNU Lesser General Public License",
		" * along with PacketWrapper.  If not, see <http://www.gnu.org/licenses/>.",
		" */"
	};

	private CodePacketReader codeReader;

	private Set<String> ignoreArray = new HashSet<String>(Arrays.asList("array", "of"));
	private WikiPacketReader wikiReader;

	public WrapperGenerator(CodePacketReader codeReader, WikiPacketReader wikiReader) {
		this.codeReader = codeReader;
		this.wikiReader = wikiReader;
	}

	public String generateClass(PacketType type) throws IOException {
		StringBuilder builder = new StringBuilder();
		IndentBuilder indent = new IndentBuilder(builder, 1);

		CodePacketInfo codeInfo = codeReader.readPacket(type);
		WikiPacketInfo wikiInfo = wikiReader.readPacket(type);

		// Java style
		String className = "Wrapper" + Wrappit.getCamelCase(type.getProtocol()) + Wrappit.getCamelCase(type.getSender())
				+ Wrappit.getCamelCase(type.name());

		// Current field index
		int fieldIndex = 0;

		for (String header : HEADER) {
			builder.append(header + NEWLN);
		}

		builder.append("package com.comphenix.packetwrapper;" + NEWLN + NEWLN);
		builder.append("import com.comphenix.protocol.PacketType;" + NEWLN);
		builder.append("import com.comphenix.protocol.events.PacketContainer;" + NEWLN + NEWLN);
		builder.append("public class " + className + " extends AbstractPacket {" + NEWLN + NEWLN);

		indent.appendLine("public static final PacketType TYPE = " + getReference(type) + ";");
		indent.appendLine("");

		// Default constructors
		indent.appendLine("public " + className + "() {");
		indent.incrementIndent().appendLine("super(new PacketContainer(TYPE), TYPE);").appendLine("handle.getModifier().writeDefaults();");
		indent.appendLine("}" + NEWLN);

		// And the wrapped packet constructor
		indent.appendLine("public " + className + "(PacketContainer packet) {");
		indent.incrementIndent().appendLine("super(packet, TYPE);");
		indent.appendLine("}" + NEWLN);

		for (WikiPacketField field : wikiInfo.getPacketFields()) {
			if (fieldIndex < codeInfo.getNetworkOrder().size()) {
				Field codeField = codeInfo.getNetworkOrder().get(fieldIndex);
				Modifiers modifier = Modifiers.getByInputType(codeField.getType());

				if (modifier == null) {
					indent.appendLine("// Cannot find type for " + codeField.getName());
					System.err.println("Cannot find type " + codeField.getType() + " for field " + codeField.getName());
					continue;
				}

				try {
					writeGetMethod(indent, fieldIndex, modifier, codeInfo, field);
				} catch (Throwable ex) {
					indent.appendLine("// Cannot generate getter " + codeField.getName());
					System.err.println("Failed to generate getter " + codeField.getName());
					ex.printStackTrace();
				}

				try {
					writeSetMethod(indent, fieldIndex, modifier, codeInfo, field);
				} catch (Throwable ex) {
					indent.appendLine("// Cannot generate setter " + codeField.getName());
					System.err.println("Failed to generate setter " + codeField.getName());
					ex.printStackTrace();
				}
			} else {
				indent.appendLine("// Cannot generate field " + field.getFieldName());
			}

			fieldIndex++;
		}

		builder.append("}");
		return builder.toString();
	}

	private String getFieldName(WikiPacketField field) {
		String converted = CaseFormating.toCamelCase(field.getFieldName());
		return converted.replace("Eid", "EntityID");
	}

	private String getFieldType(WikiPacketField field) {
		// Most are primitive
		String type = field.getFieldType().toLowerCase();

		// Better names
		type = type.replace("string", "String")
				.replace("slot", "ItemStack")
				.replace("metadata", "WrappedDataWatcher")
				.replace("unsigned", "")
				.replace("varint", "int")
				.replace("bool", "boolean")
				.replace("uuid", "UUID")
				.replace(" ", "");

		// Detect arrays
		if (type.contains("array")) {
			return (getLongestWord(type.split("\\s+"), ignoreArray).replace("array", "") + "[]").replace("of", "");
		} else {
			return type;
		}
	}

	private String getLongestWord(String[] input, Set<String> blacklist) {
		int selected = 0;

		// Find a longer word that is not on the black list
		for (int i = 1; i < input.length; i++) {
			if (!blacklist.contains(input[i]) && input[i].length() > input[selected].length()) {
				selected = i;
			}
		}
		return input[selected];
	}

	private String getModifierCall(int fieldIndex, String callFormat, CodePacketInfo codeInfo) {
		Field field = codeInfo.getNetworkOrder().get(fieldIndex);
		int memoryIndex = 0;

		// Find the correct index
		for (Field compare : codeInfo.getMemoryOrder()) {
			if (compare.getType().equals(field.getType())) {
				if (field.equals(compare))
					break;
				else
					memoryIndex++;
			}
		}

		// The modifier we will use
		Modifiers modifier = Modifiers.getByInputType(field.getType());
		String method = modifier != null ? modifier.getMethodName() : "UNKNOWN()";

		return method + String.format(callFormat, memoryIndex);
	}

	private String getReference(PacketType type) {
		return "PacketType." + Wrappit.getCamelCase(type.getProtocol()) + "." + Wrappit.getCamelCase(type.getSender()) + "." + type.name();
	}

	private void writeGetMethod(IndentBuilder indent, int fieldIndex, Modifiers modifier, CodePacketInfo codeInfo, WikiPacketField field)
			throws IOException {
		String name = getFieldName(field);
		String outputType = getFieldType(field);
		String casting = "";

		// Simple attempt at casting
		if (modifier.isWrapper()) {
			outputType = modifier.getOutputType();
		} else if (!modifier.getOutputType().equals(outputType)) {
			casting = " (" + outputType + ")";
		}

		// Pattern I noticed fixing wrappers
		if ((modifier.getOutputType().equalsIgnoreCase("int") || modifier.getOutputType().equalsIgnoreCase("float"))
				&& (outputType.equalsIgnoreCase("byte") || outputType.equalsIgnoreCase("short"))) {
			outputType = modifier.getOutputType();
			casting = "";
		}

		String note = CaseFormating.toLowerCaseRange(field.getNotes(), 0, 1).trim();

		// Comment
		indent.appendLine("/**");
		indent.appendLine(" * Retrieve " + field.getFieldName() + ".");
		if (!note.isEmpty()) {
			indent.appendLine(" * <p>");
			indent.appendLine(" * Notes: " + note);
		}
		indent.appendLine(" * @return The current " + field.getFieldName());
		indent.appendLine(" */");

		indent.appendLine("public " + outputType + " get" + name + "() {");
		indent.incrementIndent().appendLine("return" + casting + " handle." + getModifierCall(fieldIndex, ".read(%s);", codeInfo));
		indent.appendLine("}\n");
	}

	private void writeSetMethod(IndentBuilder indent, int fieldIndex, Modifiers modifier, CodePacketInfo codeInfo, WikiPacketField field)
			throws IOException {
		String name = getFieldName(field);
		String inputType = getFieldType(field);
		String casting = "";

		if (modifier.isWrapper()) {
			inputType = modifier.getOutputType();
		} else if (!modifier.getOutputType().equals(inputType)) {
			casting = " (" + modifier.getOutputType() + ")";
		}

		// Pattern I noticed fixing wrappers
		if ((modifier.getOutputType().equalsIgnoreCase("int") || modifier.getOutputType().equalsIgnoreCase("float"))
				&& (inputType.equalsIgnoreCase("byte") || inputType.equalsIgnoreCase("short"))) {
			inputType = modifier.getOutputType();
			casting = "";
		}

		// String note = CaseFormating.toLowerCaseRange(field.getNotes(), 0, 1).trim();

		// Comment
		indent.appendLine("/**");
		indent.appendLine(" * Set " + field.getFieldName() + ".");
		indent.appendLine(" * @param value - new value.");
		indent.appendLine(" */");

		indent.appendLine("public void set" + name + "(" + inputType + " value) {");
		indent.incrementIndent().appendLine("handle." + getModifierCall(fieldIndex, ".write(%s," + casting + " value);", codeInfo));
		indent.appendLine("}\n");
	}
}
