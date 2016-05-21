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
package com.comphenix.wrappit.utils;

import java.io.IOException;

/**
 * Wrapper for any given StringBuilder or StringBuffer that automatically indents
 * text by a given amount.
 * <p>
 * Only supports UNIX and Windows style line endings.
 * 
 * @author Kristian
 */
public class IndentBuilder implements Appendable {
	public static String NEWLN = System.getProperty("line.separator");
	
	private final Appendable delegate;
	private final int indentLevel;
	private final String indentText;
	
	private boolean outstandingIndent;
	
	public IndentBuilder(Appendable delegate, int indentLevel) {
		this(delegate, indentLevel, "    ");
	}
	
	public IndentBuilder(Appendable delegate, int indentLevel, String indentText) {
		this.delegate = delegate;
		this.indentLevel = indentLevel;
		this.indentText = indentText;
		this.outstandingIndent = true;
	}

	public IndentBuilder withIndent(int level) {
		return new IndentBuilder(delegate, level, indentText);
	}
	
	public IndentBuilder incrementIndent() {
		return withIndent(indentLevel + 1);
	}
	
	public IndentBuilder appendLine(CharSequence csq) throws IOException {
		append(csq);
		append(NEWLN);
		return this;
	}
	
	@Override
	public Appendable append(CharSequence csq) throws IOException {
		return append(csq, 0, csq.length());
	}

	@Override
	public Appendable append(CharSequence csq, int start, int end) throws IOException {
		for (int i = start; i < end; i++) {
			append(csq.charAt(i));
		}
		
		return this;
	}

	@Override
	public Appendable append(char c) throws IOException {
		if (outstandingIndent) {
			for (int i = 0; i < indentLevel; i++) {
				delegate.append(indentText);
			}
			outstandingIndent = false;
		}
		
		delegate.append(c);
		
		// Schedule an indent!
		if (c == '\n') {
			outstandingIndent = true;
		}
		return this;
	}
}
