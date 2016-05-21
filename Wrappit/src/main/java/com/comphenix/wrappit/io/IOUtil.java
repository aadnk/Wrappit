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
package com.comphenix.wrappit.io;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;

/**
 * Util for dealing with IO stuff
 *
 * @author dmulloy2
 */

public class IOUtil
{
	private IOUtil() { }

	/**
	 * Parses the lines of a given file in the proper order.
	 *
	 * @param file File to parse
	 * @return The lines
	 * @throws IOException If parsing fails
	 */
	public static List<String> readLines(File file) throws IOException
	{
		Validate.notNull(file, "file cannot be null!");

		Closer closer = new Closer();
		FileInputStream fis = closer.register(new FileInputStream(file));
		DataInputStream dis = closer.register(new DataInputStream(fis));
		InputStreamReader isr = closer.register(new InputStreamReader(dis));
		BufferedReader br = closer.register(new BufferedReader(isr));

		List<String> lines = new ArrayList<>();

		String line = null;
		while ((line = br.readLine()) != null)
			lines.add(line);

		closer.close();
		return lines;
	}

	/**
	 * Writes given lines to a given file in the proper order.
	 *
	 * @param file File to write to
	 * @param lines Lines to write
	 * @throws IOException If writing fails
	 */
	public static void writeLines(File file, List<String> lines) throws IOException
	{
		Validate.notNull(file, "file cannot be null!");
		Validate.notNull(lines, "lines cannot be null!");

		Closer closer = new Closer();
		FileWriter fw = closer.register(new FileWriter(file));
		PrintWriter pw = closer.register(new PrintWriter(fw));

		for (String line : lines)
			pw.println(line);

		closer.close();
	}

	/**
	 * Returns the given {@link File}'s name with the extension omitted.
	 *
	 * @param file {@link File}
	 * @param extension File extension
	 * @return The file's name with the extension omitted
	 */
	public static String trimFileExtension(File file, String extension)
	{
		Validate.notNull(file, "file cannot be null!");
		Validate.notNull(extension, "extension cannot be null!");

		int index = file.getName().lastIndexOf(extension);
		return index > 0 ? file.getName().substring(0, index) : file.getName();
	}
}
