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
package com.comphenix.wrappit.test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.reflect.FieldUtils;
import com.comphenix.protocol.utility.Constants;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.wrappit.Wrappit;
import com.comphenix.wrappit.io.Closer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import net.minecraft.server.v1_15_R1.DispenserRegistry;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;

import static org.mockito.Mockito.*;

/**
 * @author dmulloy2
 */

public class WrapperTest {
	private static boolean initialized;

	public static void test(File packetWrapper) {
		try (Closer closer = new Closer()) {
			init();
			System.out.println("Determining classes...");

			List<String> classes = new ArrayList<>();
			JarFile jar = closer.register(new JarFile(packetWrapper));

			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String className = entry.getName();
				if (className.endsWith(".class") && ! className.contains("$") && ! className.contains("PacketWrapper")) {
					className = className.replaceAll("/", ".");
					className = className.replaceAll(".class", "");
					classes.add(className);
				}
			}

			System.out.println("Successfully determined classes.");

			System.out.println("Initializing fake server...");
			WrapperTest.init();

			System.out.println("Loading classes...");
			URL[] urls = { new URL("jar:file:" + packetWrapper + "!/") };
			URLClassLoader loader = URLClassLoader.newInstance(urls);

			List<String> classNames = new ArrayList<>();
			List<String> failures = new ArrayList<>();
			for (String name : classes) {
				if (name.endsWith("WrapperPlayServerCombatEvent")) {
					// TODO Look into effectively testing this wrapper
					continue;
				}

				try {
					Class<?> clazz = loader.loadClass(name);
					classNames.add(clazz.getSimpleName());

					System.out.println("Testing " + clazz.getName() + "...");

					Constructor<?> ctor = clazz.getConstructor();
					Object instance = ctor.newInstance();
					for (Method method : clazz.getMethods()) {
						try {
							method.setAccessible(true);
							if (method.getDeclaringClass().equals(clazz) && method.getParameterTypes().length == 0) {
								System.out.println("Invoking " + method.getName());
								if (method.getName().equals("getContents")) {
									clazz.getMethod("setContentsBuffer", ByteBuf.class).invoke(instance, Unpooled.buffer());
								}

								method.invoke(instance);

								if (method.getName().equalsIgnoreCase("getEntityID")) {
									if (method.getName().equals("getEntityId")) {
										System.err.println(clazz.getName() + " :: " + method.getName() + " is improperly cased!");
									} else {
										try {
											clazz.getMethod("getEntity", World.class);
										} catch (Throwable ex) {
											System.err.println(clazz.getName() + " does not specify a getEntity(World) method!");
										}
									}
								}
							}
						} catch (Throwable ex) {
							System.err.println("Failed to invoke method " + method + ":");
							ex.printStackTrace();

							failures.add(clazz.getName() + " :: " + method.getName() + " :: " + ex.getCause());
						}
					}
				} catch (NoSuchMethodException ex) {
				} catch (Throwable ex) {
					System.err.println("Failed to test " + name + ":");
					ex.printStackTrace();
				}
			}

			System.out.println("Done!");
			if (failures.size() > 0) {
				System.out.println("Encountered " + failures.size() + " failures:");
				for (String failure : failures) {
					System.out.println("  " + failure);
				}
			}

			failures.clear();
			System.out.println("Ensuring wrappers for all packet types exist...");

			for (PacketType type : PacketType.values()) {
				String className = "Wrapper" + Wrappit.getCamelCase(type.getProtocol()) + Wrappit.getCamelCase(type.getSender())
						+ Wrappit.getCamelCase(type.name());
				if (! classNames.contains(className)) {
					if (type.isDeprecated()) {
						System.out.println("Wrapper does not exist for deprecated type: " + type);
					} else if (type.isDynamic()) {
						System.out.println("PacketType does not exist for packet class: " + type.getPacketClass().getSimpleName());
					} else {
						System.err.println("Wrapper does not exist for packet: " + type.toString());
						failures.add(className);
					}
				}
			}

			System.out.println("Done!");
			if (failures.size() > 0) {
				System.out.println("Encountered " + failures.size() + " missing wrappers:");
				for (String failure : failures) {
					System.out.println("  " + failure);
				}
			}
		} catch (Throwable ex) {
			System.err.println("Failed to test PacketWrapper:");
			ex.printStackTrace();
		}
	}

	private static void init() throws Throwable {
		if (!initialized) {
			// Denote that we're done
			initialized = true;

			initPackage();

			DispenserRegistry.init();

			// Mock the server object
			Server mockedServer = mock(Server.class);
			ItemMeta mockedMeta = mock(ItemMeta.class);
			ItemFactory mockedFactory = new ItemFactoryDelegate(mockedMeta);

			when(mockedServer.getItemFactory()).thenReturn(mockedFactory);
			when(mockedServer.isPrimaryThread()).thenReturn(true);
			// when(mockedFactory.getItemMeta(any(Material.class))).thenReturn(mockedMeta);

			// Inject this fake server
			FieldUtils.writeStaticField(Bukkit.class, "server", mockedServer, true);
		}
	}

	private static void initPackage() {
		// Initialize reflection
		MinecraftReflection.setMinecraftPackage(Constants.NMS, Constants.OBC);
		MinecraftVersion.setCurrentVersion(MinecraftVersion.COLOR_UPDATE);
	}
}
