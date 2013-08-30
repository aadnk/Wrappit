package com.comphenix.wrappit.minecraft;

import java.lang.reflect.Field;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class FieldEnumerator extends MethodVisitor {
	boolean newLine = false;
	private Class<?> packetClass;
	private String className;
	
	protected List<Field> output;
	
	public FieldEnumerator(Class<?> packetClass, List<Field> output) {
		super(Opcodes.ASM4);
		this.packetClass = packetClass;
		this.className = packetClass.getCanonicalName().replace(".", "/");
		
		// The output
		this.output = output;
	}
	
	@Override
	public void visitLineNumber(int line, Label start) {
		newLine = true;
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		// We are only interested in instance fields
		if (opcode == Opcodes.GETFIELD) {
			// Only accept the first GETFIELD in a line
			if (newLine)
				newLine = false;
			else
				return;
			
			try {
				if (owner.equals(className)) {
					Field field = getField(packetClass, name);
					
					// Skip static fields here too
					if (processField(field)) {
						output.add(field);
					}
				}
				
			} catch (Exception e) {
				throw new RuntimeException("Error", e);
			}
		}
	}
	
	/**
	 * Accept all by default.
	 * @param field - field to filter.
	 */
	protected boolean processField(Field field) {
		return true;
	}
		
	/**
	 * Retrieve a field by searching through the inheritance chain.
	 * @param clazz - the class to start looking. 
	 * @param name - name of the field to find.
	 * @return The resulting field.
	 */
	private Field getField(Class<?> clazz, String name) {
		// Go through every defined field of every class in the hierachy
		for (; clazz != null; clazz = clazz.getSuperclass()) {
			for (Field field : clazz.getDeclaredFields()) {
				if (field.getName().equals(name))
					return field;
			}
		}

		throw new NoSuchFieldError(name);
	}
}