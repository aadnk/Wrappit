/**
 * Copyright (C) dmulloy2 <http://dmulloy2.net>
 * Copyright (C) Kristian S. Strangeland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.comphenix.wrappit.minecraft;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.utility.Constants;

public class CodePacketReader {
	// Write packet method signature
	private static final String WRITE_PACKET_SIGNATURE = "(Lnet/minecraft/server/" + Constants.PACKAGE_VERSION + "/PacketDataSerializer;)V";
	private static final String WRITE_PACKET_NAME = "b";
	
	/**
	 * Read a particular packet from local code.
	 * @param type - the type of the packet to read.
	 * @return The resulting packet information.
	 * @throws IOException If we are unable to parse the network order.
	 */
	public CodePacketInfo readPacket(PacketType type) throws IOException {
		Class<?> packetClass = type.getPacketClass();
		
		if (packetClass != null) {
			List<Field> memoryOrder = readMemoryOrder(packetClass);
			List<Field> networkOrder = readNetworkOrder(packetClass);
			return new CodePacketInfo(memoryOrder, networkOrder, type);
		} else {
			throw new IllegalArgumentException("Packet " + type + " is not registered.");
		}
	}
	
	private List<Field> readMemoryOrder(Class<?> packetClass) {
		final List<Field> result = new ArrayList<>();
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
		final List<Field> result = new ArrayList<>();

		reader.accept(new ClassVisitor(Opcodes.ASM4) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				final String writePacketName = name;
				
				// Is this our write packet method?
				if (desc.equals(WRITE_PACKET_SIGNATURE) && writePacketName.equals(WRITE_PACKET_NAME)) {
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
			   !field.getDeclaringClass().getSimpleName().equals("Packet");
	}
	
	/**
	 * Compute the union of every given array.
	 * @param array - array of arrays.
	 * @return Set containing the union of all the arrays.
	 */
	@SafeVarargs
	private static <T> Set<T> setUnion(T[]... array) {
		Set<T> result = new LinkedHashSet<>();
		
		for (T[] elements : array) {
			Collections.addAll(result, elements);
		}
		
		return result;
	}
}
