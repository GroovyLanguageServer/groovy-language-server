////////////////////////////////////////////////////////////////////////////////
// Copyright 2022 Prominic.NET, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License
//
// Author: Prominic.NET, Inc.
// No warranty of merchantability or fitness of any kind.
// Use this software at your own risk.
////////////////////////////////////////////////////////////////////////////////
package net.prominic.lsp.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Comparator;

import org.eclipse.lsp4j.Position;

public class Positions {
	public static final Comparator<Position> COMPARATOR = (Position p1, Position p2) -> {
		if (p1.getLine() != p2.getLine()) {
			return p1.getLine() - p2.getLine();
		}
		return p1.getCharacter() - p2.getCharacter();
	};

	public static boolean valid(Position p) {
		return p.getLine() >= 0 || p.getCharacter() >= 0;
	}

	public static int getOffset(String string, Position position) {
		int line = position.getLine();
		int character = position.getCharacter();
		int currentIndex = 0;
		if (line > 0) {
			BufferedReader reader = new BufferedReader(new StringReader(string));
			try {
				int readLines = 0;
				while (true) {
					char currentChar = (char) reader.read();
					if (currentChar == -1) {
						return -1;
					}
					currentIndex++;
					if (currentChar == '\n') {
						readLines++;
						if (readLines == line) {
							break;
						}
					}
				}
			} catch (IOException e) {
				return -1;
			}
			try {
				reader.close();
			} catch (IOException e) {
			}
		}
		return currentIndex + character;
	}
}