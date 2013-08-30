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
	public static String newline = "\n";
	
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
		append(newline);
		return this;
	}
	
	public Appendable append(CharSequence csq) throws IOException {
		return append(csq, 0, csq.length());
	}

	public Appendable append(CharSequence csq, int start, int end) throws IOException {
		for (int i = start; i < end; i++) {
			append(csq.charAt(i));
		}
		
		return this;
	}

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
