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

import net.minecraft.server.v1_8_R1.Block;
import net.minecraft.server.v1_8_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R1.DataWatcher;
import net.minecraft.server.v1_8_R1.IChatBaseComponent;
import net.minecraft.server.v1_8_R1.ItemStack;
import net.minecraft.server.v1_8_R1.NBTTagCompound;
import net.minecraft.server.v1_8_R1.ServerPing;
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
		BOOLEANS(boolean.class, 				 		"boolean", 			 		"getSpecificModifier(boolean.class)"),
		BYTES(byte.class, 						 		"byte", 			 		"getBytes()"),
		SHORTS(short.class, 					 		"short", 			 		"getShorts()"),
		INTEGERS(int.class, 					 		"int", 				 		"getIntegers()"),
		LONGS(long.class, 						 		"long", 			 		"getLongs()"),
		FLOATS(float.class, 					 		"float", 			 		"getFloat()"),
		DOUBLES(double.class, 					 		"double", 			 		"getDoubles()"),
		ENUMS(Enum.class, 						 		"Enum<?>", 			 		"getSpecificModifier(Enum.class)"),
		STRINGS(String.class, 					 		"String", 			 		"getStrings()"),
		STRING_ARRAYS(String[].class, 			 		"String[]", 		 		"getStringArrays()"),
		BYTE_ARRAYS(byte[].class, 				 		"byte[]", 			 		"getByteArrays()"),
		INTEGER_ARRAYS(int[].class, 					"int[]", 			 		"getIntegerArrays()"),
		ITEM_MODIFIER(ItemStack.class, 					"ItemStack", 		 		"getItemModifier()"),
		ITEM_ARRAY_MODIFIER(ItemStack[].class,			"ItemStack[]", 		 		"getItemArrayModifier()"),
		WORLD_TYPE_MODIFIER(WorldType.class, 			"WorldType", 				"getWorldTypeModifier()"),
		DATA_WATCHER_MODIFIER(DataWatcher.class, 		"WrappedDataWatcher", 		"getDataWatcherModifier()"),
		POSITION_MODIFIER(List.class, 			 		"List<ChunkPosition>",  	"getPositionCollectionModifier()"),
		NBT_MODIFIER(NBTTagCompound.class, 	  	 		"NbtBase<?>", 				"getNbtModifier()"),
		MAP(Map.class, 	  	 					 		"Map<?,?>", 				"getSpecificModifier(Map.class)"),
		PUBLIC_KEY_MODIFIER(PublicKey.class, 	 		"PublicKey", 				"getSpecificModifier(PublicKey.class)"),
		CHAT_BASE_COMPONENT(IChatBaseComponent.class, 	"WrappedChatComponent",		"getChatComponents()"),
		GAME_PROFILE(GameProfile.class, 				"WrappedGameProfile",		"getGameProfiles()"),
		SERVER_PING(ServerPing.class,					"WrappedServerPing",		"getServerPings()"),
		BLOCK(Block.class,								"Material",					"getBlocks()"),
		CHUNK_COORD_INT_PAIR(ChunkCoordIntPair.class,	"ChunkCoordIntPair",		"getChunkCoordIntPairs()"),
		COMPONENT_ARRAY(IChatBaseComponent[].class,		"IChatBaseComponent[]",		"getChatComponentArrays()");

		private Class<?> inputType;
		private String outputType;
		private String name;

		private static Map<Class<?>, Modifiers> inputLookup;

		static {
			inputLookup = new HashMap<Class<?>, Modifiers>();

			for (Modifiers modifier : values()) {
				inputLookup.put(modifier.inputType, modifier);
			}
		}

		private Modifiers(Class<?> inputType, String outputType, String name) {
			this.inputType = inputType;
			this.outputType = outputType;
			this.name = name;
		}

		public boolean isWrapper() {
			return this == DATA_WATCHER_MODIFIER || this == CHAT_BASE_COMPONENT || this == POSITION_MODIFIER || this == GAME_PROFILE || this == SERVER_PING || this == BLOCK || this == CHUNK_COORD_INT_PAIR;
		}

		public Class<?> getInputType() {
			return inputType;
		}

		public String getOutputType() {
			return outputType;
		}

		public String getMethodName() {
			return name;
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
	}

	private Set<String> ignoreArray = new HashSet<String>(Arrays.asList("array", "of"));

	private CodePacketReader codeReader;
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
		String className = "Wrapper" + Wrappit.getCamelCase(type.getProtocol()) + Wrappit.getCamelCase(type.getSender()) + Wrappit.getCamelCase(type.name());

		// Current field index
		int fieldIndex = 0;

		builder.append("package com.comphenix.packetwrapper;\n\n");
		builder.append("import com.comphenix.protocol.PacketType;\n");
		builder.append("import com.comphenix.protocol.events.PacketContainer;\n\n");
		builder.append("public class " + className + " extends AbstractPacket {\n");

		indent.appendLine("public static final PacketType TYPE = " + getReference(type) + ";");
		indent.appendLine("");

		// Default constructors
		indent.appendLine("public " + className + "() {");
		indent.incrementIndent().appendLine("super(new PacketContainer(TYPE), TYPE);").appendLine("handle.getModifier().writeDefaults();");
		indent.appendLine("}\n");

		// And the wrapped packet constructor
		indent.appendLine("public " + className + "(PacketContainer packet) {");
		indent.incrementIndent().appendLine("super(packet, TYPE);");
		indent.appendLine("}\n");

		for (WikiPacketField field : wikiInfo.getPacketFields()) {
			if (fieldIndex < codeInfo.getNetworkOrder().size()) {
				Field codeField = codeInfo.getNetworkOrder().get(fieldIndex);
				Modifiers modifier = Modifiers.getByInputType(codeField.getType());

				if (modifier == null) {
					throw new IllegalArgumentException("Cannot find modifier for " + codeField.getType());
				}

				writeGetMethod(indent, fieldIndex, modifier, codeInfo, field);
				writeSetMethod(indent, fieldIndex, modifier, codeInfo, field);
			} else {
				indent.appendLine("// Cannot generate field " + field.getFieldName());
			}

			fieldIndex++;
		}

		builder.append("}\n");
		return builder.toString();
	}

	private String getReference(PacketType type) {
		return "PacketType." + Wrappit.getCamelCase(type.getProtocol()) + "." + Wrappit.getCamelCase(type.getSender()) + "." + type.name();
	}

	private String getFieldType(WikiPacketField field) {
		// Most are primitive
		String type = field.getFieldType().toLowerCase();

		// Better names
		type = type.replace("string", "String").replace("slot", "ItemStack").replace("metadata", "WrappedDataWatcher").replace("unsigned", "").replace("varint", "int").replace(" ", "");

		// Detect arrays
		if (type.contains("array")) {
			return getLongestWord(type.split("\\s+"), ignoreArray).replace("array", "") + "[]";
		} else {
			return type;
		}
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

	private String getFieldName(WikiPacketField field) {
		String converted = CaseFormating.toCamelCase(field.getFieldName());
		return converted.replace("Eid", "EntityID");
	}

	private void writeGetMethod(IndentBuilder indent, int fieldIndex, Modifiers modifier, CodePacketInfo codeInfo, WikiPacketField field) throws IOException {
		String name = getFieldName(field);
		String outputType = getFieldType(field);
		String casting = "";

		// Simple attempt at casting
		if (modifier.isWrapper()) {
			outputType = modifier.getOutputType();
		} else if (!modifier.getOutputType().equals(outputType)) {
			casting = " (" + outputType + ")";
		}

		// Comment
		indent.appendLine("/**");
		indent.appendLine(" * Retrieve " + CaseFormating.toLowerCaseRange(field.getNotes(), 0, 1) + ".");
		indent.appendLine(" * @return The current " + field.getFieldName());
		indent.appendLine("*/");

		indent.appendLine("public " + outputType + " get" + name + "() {");
		indent.incrementIndent().appendLine("return" + casting + " handle." + getModifierCall(fieldIndex, ".read(%s);", codeInfo));
		indent.appendLine("}\n");
	}

	private void writeSetMethod(IndentBuilder indent, int fieldIndex, Modifiers modifier, CodePacketInfo codeInfo, WikiPacketField field) throws IOException {
		String name = getFieldName(field);
		String inputType = getFieldType(field);
		String casting = "";

		if (modifier.isWrapper()) {
			inputType = modifier.getOutputType();
		} else if (!modifier.getOutputType().equals(inputType)) {
			casting = " (" + modifier.getOutputType() + ")";
		}

		// Comment
		indent.appendLine("/**");
		indent.appendLine(" * Set " + CaseFormating.toLowerCaseRange(field.getNotes(), 0, 1) + ".");
		indent.appendLine(" * @param value - new value.");
		indent.appendLine("*/");

		indent.appendLine("public void set" + name + "(" + inputType + " value) {");
		indent.incrementIndent().appendLine("handle." + getModifierCall(fieldIndex, ".write(%s," + casting + " value);", codeInfo));
		indent.appendLine("}\n");
	}
}
