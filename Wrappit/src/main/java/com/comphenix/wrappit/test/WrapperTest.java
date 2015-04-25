/**
 * (c) 2015 dmulloy2
 */
package com.comphenix.wrappit.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

import net.minecraft.server.v1_8_R2.DispenserRegistry;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;

import com.comphenix.protocol.reflect.FieldUtils;
import com.comphenix.protocol.utility.Constants;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.wrappit.io.Closer;

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

			List<String> failures = new ArrayList<>();
			for (String name : classes) {
				try {
					Class<?> clazz = loader.loadClass(name);
					System.out.println("Testing " + clazz.getName() + "...");

					Constructor<?> ctor = clazz.getConstructor();
					Object instance = ctor.newInstance();
					for (Method method : clazz.getMethods()) {
						try {
							method.setAccessible(true);
							if (! method.getDeclaringClass().equals(Object.class) && method.getParameterTypes().length == 0) {
								System.out.println("Invoking " + method.getName());
								method.invoke(instance);
							}
						} catch (Throwable ex) {
							Throwable cause = ex.getCause();
							if (!(cause instanceof IllegalStateException)) {
								System.err.println("Failed to invoke method " + method + ":");
								ex.printStackTrace();

								failures.add(clazz.getName() + " :: " + method.getName() + " :: " + cause);
							}
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
					System.out.println(failure);
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

			DispenserRegistry.c(); // Basically registers everything

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
		MinecraftVersion.setCurrentVersion(MinecraftVersion.BOUNTIFUL_UPDATE);
	}
}