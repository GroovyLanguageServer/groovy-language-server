////////////////////////////////////////////////////////////////////////////////
// Copyright 2019 Prominic.NET, Inc.
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
package net.prominic.groovyls.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;

public class FileContentsTracker {

	private Map<URI, String> openFiles = new HashMap<>();

	public Set<URI> getOpenURIs() {
		return openFiles.keySet();
	}

	public boolean isOpen(URI uri) {
		return openFiles.containsKey(uri);
	}

	public void didOpen(DidOpenTextDocumentParams params) {
		URI uri = URI.create(params.getTextDocument().getUri());
		openFiles.put(uri, params.getTextDocument().getText());
	}

	public void didChange(DidChangeTextDocumentParams params) {
		URI uri = URI.create(params.getTextDocument().getUri());
		openFiles.put(uri, params.getContentChanges().get(0).getText());
	}

	public void didClose(DidCloseTextDocumentParams params) {
		URI uri = URI.create(params.getTextDocument().getUri());
		openFiles.remove(uri);
	}

	public String getContents(URI uri) {
		if (!openFiles.containsKey(uri)) {
			BufferedReader reader = null;
			try {
				reader = Files.newBufferedReader(Paths.get(uri));
				StringBuilder builder = new StringBuilder();
				int next = -1;
				while ((next = reader.read()) != -1) {
					builder.append((char) next);
				}
				return builder.toString();
			} catch (IOException e) {
				return null;
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
					}
				}
			}
		}
		return openFiles.get(uri);
	}
}