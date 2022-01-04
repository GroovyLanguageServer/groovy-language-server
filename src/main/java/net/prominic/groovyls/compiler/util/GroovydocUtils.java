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
package net.prominic.groovyls.compiler.util;

import groovy.lang.groovydoc.Groovydoc;

public class GroovydocUtils {
	public static String groovydocToMarkdownDescription(Groovydoc groovydoc) {
		if (groovydoc == null || !groovydoc.isPresent()) {
			return null;
		}
		String content = groovydoc.getContent();
		String[] lines = content.split("\n");
		StringBuilder markdownBuilder = new StringBuilder();
		int n = lines.length;
		if (n == 1) {
			// strip end of asdoc comment
			int c = lines[0].indexOf("*/");
			if (c != -1) {
				lines[0] = lines[0].substring(0, c);
			}
		}
		// strip start of asdoc coment
		String line = lines[0];
		int lengthToRemove = Math.min(line.length(), 3);
		line = line.substring(lengthToRemove);
		markdownBuilder.append(line);
		for (int i = 1; i < n - 1; i++) {
			line = lines[i];
			int star = line.indexOf("*");
			int at = line.indexOf("@");
			if (at == -1) {
				if (star > -1) // line starts with a *
				{
					markdownBuilder.append(line.substring(star + 1));
				}
			}
		}
		return markdownBuilder.toString().trim();
	}
}
