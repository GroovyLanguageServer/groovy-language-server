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
package net.prominic.groovyls.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;

import net.prominic.lsp.utils.Positions;

public class FileContentsTracker {

	private Map<URI, String> openFiles = new HashMap<>();
	private Set<URI> changedFiles = new HashSet<>();

	public Set<URI> getOpenURIs() {
		return openFiles.keySet();
	}

	public Set<URI> getChangedURIs() {
		return changedFiles;
	}

	public void resetChangedFiles() {
		changedFiles = new HashSet<>();
	}

	public void forceChanged(URI uri) {
		changedFiles.add(uri);
	}

	public boolean isOpen(URI uri) {
		return openFiles.containsKey(uri);
	}

	public void didOpen(DidOpenTextDocumentParams params) {
		URI uri = URI.create(params.getTextDocument().getUri());
		openFiles.put(uri, params.getTextDocument().getText());
		changedFiles.add(uri);
	}

	public void didChange(DidChangeTextDocumentParams params) {
		URI uri = URI.create(params.getTextDocument().getUri());
		String oldText = openFiles.get(uri);
		TextDocumentContentChangeEvent change = params.getContentChanges().get(0);
		Range range = change.getRange();
		if (range == null) {
			openFiles.put(uri, change.getText());
		} else {
			int offsetStart = Positions.getOffset(oldText, change.getRange().getStart());
			int offsetEnd = Positions.getOffset(oldText, change.getRange().getEnd());
			StringBuilder builder = new StringBuilder();
			builder.append(oldText.substring(0, offsetStart));
			builder.append(change.getText());
			builder.append(oldText.substring(offsetEnd));
			openFiles.put(uri, builder.toString());
		}
		changedFiles.add(uri);
	}

	public void didClose(DidCloseTextDocumentParams params) {
		URI uri = URI.create(params.getTextDocument().getUri());
		openFiles.remove(uri);
		changedFiles.add(uri);
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

	public void setContents(URI uri, String contents) {
		openFiles.put(uri, contents);
	}
}