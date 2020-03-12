/* 
 * JPUtil
 * Copyright (C) 2019  Javapony/OLEGSHA
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package ru.windcorp.jputil.textui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

// SonarLint: Standard outputs should not be used directly to log anything (java:S106)
//   This is intended for simple console applications that use System.out directly 
@SuppressWarnings("squid:S106")

public class TextUI {
	
	private TextUI() {}
	
	public static final BufferedReader SYS_IN_READER = new BufferedReader(new InputStreamReader(System.in));
	
	public static String readLine() {
		try {
			return SYS_IN_READER.readLine();
		} catch (IOException e) {
			throw new UncheckedIOException("Exception while reading from System.in", e);
		}
	}
	
	public static void write(Object obj) {
		System.out.println(obj.toString());
	}
	
	@SafeVarargs
	public static <T> T select(String query, T... options) {
		write(query);
		write("");
		for (int i = 0; i < options.length; ++i) {
			write((i + 1) + ".  " + options[i]);
		}
		write("");
		
		String awnser;
		int index; // 1 .. options.length (inclusive)
		while (true) {
			awnser = readLine();
			
			try {
				index = Integer.parseInt(awnser);
				
				if (index < 1 || index > options.length) {
					continue;
				}
				
				write("-> " + options[index - 1]);
				return options[index - 1];
			} catch (NumberFormatException e) {
				// continue
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T select(String query, Collection<T> options) {
		return (T) select(query, options.toArray());
	}
	
	public static boolean ask(String question, String yes, String no) {
		write(question);
		write("");
		write("Y - " + yes);
		write("N - " + no);
		write("");
		
		String awnser;
		while (true) {
			awnser = readLine().toUpperCase();
			if (awnser.equals("Y")) {
				write("-> " + yes);
				return true;
			} else if (awnser.equals("N")) {
				write("-> " + no);
				return false;
			} else {
				// continue
			}
		}
	}
	
	public static String prompt(String prompt) {
		write(prompt);
		return readLine();
	}
	
	public static void thickLine() {
		write("=================================================");
	}
	
	public static void thinLine() {
		write("-------------------------------------------------");
	}
	
	private static class Confirmer extends Thread {
		AtomicBoolean complete = new AtomicBoolean(false);
		AtomicBoolean run = new AtomicBoolean(true);
		
		Confirmer() {
			super("TextUI Confirmer");
		}
		
		// SonarLint: Catches should be combined (java:S2147)
		// SonarLint: "InterruptedException" should not be ignored (java:S2142)
		//   Look. At. The comments.
		@SuppressWarnings({"squid:S2147", "squid:S2142"})

		@Override
		public void run() {
			try {
				while (System.in.available() == 0 && run.get()) {
					Thread.sleep(200);
				}
				
				System.in.read();
				complete.set(true);
			} catch (IOException e) {
				// Ignore
			} catch (InterruptedException e) {
				// Ignore - we've timed out
			}
		}
		
		
	}
	
	private static class ConfirmerInterrupter extends Thread {
		Confirmer prompter;
		long milliseconds;
		
		ConfirmerInterrupter(long ms, Confirmer prompter) {
			super("TextUI Confirmer Interrupter");
			this.milliseconds = ms;
			this.prompter = prompter;
		}
		
		@Override
		public void run() {
			try {
				Thread.sleep(milliseconds);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (!prompter.complete.get()) {
				prompter.interrupt();
			}
		}
		
	}
	
	// SonarLint: "InterruptedException" should not be ignored (java:S2142)
	//   Look. At. The comments.
	@SuppressWarnings("squid:S2142")
			
	public static boolean confirm(String prompt, String pressEnter, long timeout) {

		if (prompt != null) write(prompt);
		write(pressEnter == null ? "Press ENTER to continue..." : pressEnter);
		
		Confirmer prompter = new Confirmer();
		ConfirmerInterrupter interrupter = new ConfirmerInterrupter(timeout, prompter);
		
		prompter.start();
		interrupter.start();
		
		try {
			prompter.join();
		} catch (InterruptedException e) {
			// Ignore - we've expected this
		}
		
		return prompter.complete.get();
		
	}
	
	public static void notify(String notification, String pressEnter) {
		write(notification);
		write(pressEnter == null ? "Press ENTER to continue..." : pressEnter);
		readLine();
	}
	
	public static Long readInteger(String prompt,
			String formattedNotANumber,
			boolean allowExit) {
		return readInteger(prompt, formattedNotANumber, allowExit, Long.MIN_VALUE, Long.MAX_VALUE,
				null, null);
	}

	public static Long readInteger(
			String prompt,
			String formattedNotANumber,
			boolean allowExit,
			long min, long max,
			String formattedTooSmall, String formattedTooLarge
	) {
		String line = null;
		long result;
		
		if (formattedNotANumber == null) formattedNotANumber = "\"%s\" is not an integer";
		if (formattedTooSmall   == null) formattedTooSmall   = "%d is less than %d";
		if (formattedTooLarge   == null) formattedTooLarge   = "%d is greater than %d";
		
		if (prompt != null) write(prompt);
		
		while (true) {
			try {
				
				line = readLine().trim();
				
				if (allowExit && line.isEmpty()) {
					return null;
				}
				
				result = Long.decode(line);
				
				if (result < min) {
					write(String.format(formattedTooSmall, result, min));
					continue;
				}
				
				if (result > max) {
					write(String.format(formattedTooLarge, result, max));
					continue;
				}
				
				return result;
				
			} catch (NumberFormatException e) {
				write(String.format(formattedNotANumber, line));
			}
		}
	}
	
	public static Double readFPNumber(
			String prompt,
			String formattedNotANumber,
			boolean allowExit
	) {
		String line = null;
		
		if (formattedNotANumber == null) formattedNotANumber = "\"%s\" is not a number";
		
		if (prompt != null) write(prompt);
		
		while (true) {
			try {
				line = readLine().trim();
				if (allowExit && line.isEmpty()) {
					return null;
				}
				return Double.parseDouble(line);
			} catch (NumberFormatException e) {
				write(String.format(formattedNotANumber, line));
			}
		}
	}
}
