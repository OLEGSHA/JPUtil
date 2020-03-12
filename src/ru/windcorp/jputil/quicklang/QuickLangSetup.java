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
package ru.windcorp.jputil.quicklang;

import java.util.Scanner;

public class QuickLangSetup {
	
	private QuickLangSetup() {}
	
	public static void setupLang(final ClassLoader loader, final String path) {
		QLang.setLangSupplier(name -> loader.getResourceAsStream(path + "/" + name + ".lang"));
		
		try (Scanner scanner = new Scanner(loader.getResourceAsStream(path + "/langs"))) {
			while (scanner.hasNext()) {
				QLangInfo.getLangInfo(scanner.next(), scanner.next());
			}
		}
	}

}
