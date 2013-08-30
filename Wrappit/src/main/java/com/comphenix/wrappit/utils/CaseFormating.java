package com.comphenix.wrappit.utils;

public class CaseFormating {
	/**
	 * Convert a given text to camel case with no spaces.
	 * @param text - the text to convert.
	 * @return The resulting camel case.
	 */
	public static String toCamelCase(String text) {
		return toCamelCase(text, "");
	}
	
	/**
	 * Convert a given text to camel case.
	 * @param text - the text to convert.
	 * @param delimiter - the delimited used to join each word after the process.
	 * @return The resulting camel case.
	 */
	public static String toCamelCase(String text, String delimiter) {
		String[] words = text.split("\\W+");
		StringBuilder output = new StringBuilder();
		
		// Capitalize all the words
		for (int i = 0; i < words.length; i++) {
			output.append(words[i].substring(0, 1).toUpperCase()); 
			output.append(words[i].substring(1).toLowerCase());
			output.append(delimiter);
		}
		return output.toString();
	}
	
	/**
	 * Convert a part of the string to lower case. The rest is left as is.
	 * @param text - the string to convert.
	 * @param min - the index of the first character to convert to lower case (inclusive).
	 * @param max - the index of the last character to convert (exclusive).
	 * @return The converted string.
	 */
	public static String toLowerCaseRange(String text, int min, int max) {
		StringBuilder result = new StringBuilder();
		
		// Convert a range of characters
		for (int i = 0; i < text.length(); i++) {
			if (min <= i && i < max)
				result.append(Character.toLowerCase(text.charAt(i)));
			else
				result.append(text.charAt(i));
		}
		return result.toString();
	}
}
