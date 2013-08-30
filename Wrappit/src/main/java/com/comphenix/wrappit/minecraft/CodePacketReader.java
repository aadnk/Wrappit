package com.comphenix.wrappit.minecraft;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CodePacketReader {

	// Write packet method signature
	private static final String WRITE_PACKET_SIGNATURE = "(Ljava/io/DataOutput;)V";
	
	// Used to access the packet registry
	private static PacketRegistry registry = new PacketRegistry();
	
	/**
	 * Read a particular packet from local code.
	 * @param packetID - the ID of the packet to read.
	 * @return The resulting packet information.
	 * @throws IOException If we are unable to parse the network order.
	 */
	public CodePacketInfo readPacket(int packetID) throws IOException {
		Class<?> packetClass = registry.getPacketClass(packetID);
		
		if (packetClass != null) {
			List<Field> memoryOrder = readMemoryOrder(packetClass);
			List<Field> networkOrder = readNetworkOrder(packetClass);
			return new CodePacketInfo(memoryOrder, networkOrder, packetID);
		} else {
			throw new IllegalArgumentException("Packet " + packetID + " is not registered.");
		}
	}
	
	private List<Field> readMemoryOrder(Class<?> packetClass) {
		final List<Field> result = new ArrayList<Field>();
		final Set<Field> candidates = setUnion(packetClass.getDeclaredFields(), packetClass.getFields());
				
		// Use reflection to do this very simply
		for (Field field : candidates) {
			// Skip static fields
			if (isValidField(field)) {
				result.add(field);	
			}
		}
		
		return result;
	}

	private List<Field> readNetworkOrder(final Class<?> packetClass) throws IOException {
		final ClassReader reader = new ClassReader(packetClass.getCanonicalName());
		final List<Field> result = new ArrayList<Field>();

		reader.accept(new ClassVisitor(Opcodes.ASM4) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				final String writePacketName = name;
				
				// Is this our write packet method?
				if (desc.equals(WRITE_PACKET_SIGNATURE)) {
					return new FieldEnumerator(packetClass, result) {
						@Override
						public void visitMethodInsn(int opcode, String owner, String name, String desc) {
							// Super method call?
							if (opcode == Opcodes.INVOKESPECIAL) {
								if (name.equals(writePacketName) && desc.equals(WRITE_PACKET_SIGNATURE)) {
									// Add the fields written there too
									try {
										List<Field> superMethod = readNetworkOrder(packetClass.getSuperclass());
										output.addAll(superMethod);
									} catch (IOException e) {
										throw new RuntimeException("Inner I/O error", e);
									}
								}
							}
						}
						
						@Override
						protected boolean processField(Field field) {
							return isValidField(field);
						};
					};
				} else {
					return null;
				}
			}
		}, ClassReader.EXPAND_FRAMES);
		
		return result;
	}
	
	/**
	 * Determine if a field should be included in the list.
	 * @param field - the field to check.
	 * @return TRUE if it should, FALSE otherwise.
	 */
	private boolean isValidField(Field field) {
		return !Modifier.isStatic(field.getModifiers()) && 
				// And skip fields in the Packet super class
			   !field.getDeclaringClass().getSuperclass().equals(Object.class);
	}
	
	/**
	 * Compute the union of every given array.
	 * @param array - array of arrays.
	 * @return Set containing the union of all the arrays.
	 */
	private static <T> Set<T> setUnion(T[]... array) {
		Set<T> result = new LinkedHashSet<T>();
		
		for (T[] elements : array) {
			for (T element : elements) {
				result.add(element);
			}
		}
		
		return result;
	}
}
