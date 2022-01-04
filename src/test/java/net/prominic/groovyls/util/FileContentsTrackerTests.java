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

import java.net.URI;
import java.util.Collections;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileContentsTrackerTests {
	private FileContentsTracker tracker;

	@BeforeEach
	void setup() {
		tracker = new FileContentsTracker();
	}

	@AfterEach
	void tearDown() {
		tracker = null;
	}

	@Test
	void testDidOpen() {
		DidOpenTextDocumentParams params = new DidOpenTextDocumentParams();
		params.setTextDocument(new TextDocumentItem("file.txt", "plaintext", 1, "hello world"));
		tracker.didOpen(params);
		Assertions.assertEquals("hello world", tracker.getContents(URI.create("file.txt")));
	}

	@Test
	void testDidChangeWithoutRange() {
		DidOpenTextDocumentParams openParams = new DidOpenTextDocumentParams();
		openParams.setTextDocument(new TextDocumentItem("file.txt", "plaintext", 1, "hello world"));
		tracker.didOpen(openParams);
		DidChangeTextDocumentParams changeParams = new DidChangeTextDocumentParams();
		changeParams.setTextDocument(new VersionedTextDocumentIdentifier("file.txt", 2));
		TextDocumentContentChangeEvent changeEvent = new TextDocumentContentChangeEvent();
		changeEvent.setText("hi there");
		changeParams.setContentChanges(Collections.singletonList(changeEvent));
		tracker.didChange(changeParams);
		Assertions.assertEquals("hi there", tracker.getContents(URI.create("file.txt")));
	}

	@Test
	void testDidChangeWithRange() {
		DidOpenTextDocumentParams openParams = new DidOpenTextDocumentParams();
		openParams.setTextDocument(new TextDocumentItem("file.txt", "plaintext", 1, "hello world"));
		tracker.didOpen(openParams);
		DidChangeTextDocumentParams changeParams = new DidChangeTextDocumentParams();
		changeParams.setTextDocument(new VersionedTextDocumentIdentifier("file.txt", 2));
		TextDocumentContentChangeEvent changeEvent = new TextDocumentContentChangeEvent();
		changeEvent.setText(", friend");
		changeEvent.setRange(new Range(new Position(0, 5), new Position(0, 11)));
		changeEvent.setRangeLength(6);
		changeParams.setContentChanges(Collections.singletonList(changeEvent));
		tracker.didChange(changeParams);
		Assertions.assertEquals("hello, friend", tracker.getContents(URI.create("file.txt")));
	}

	@Test
	void testDidChangeWithRangeMultiline() {
		DidOpenTextDocumentParams openParams = new DidOpenTextDocumentParams();
		openParams.setTextDocument(new TextDocumentItem("file.txt", "plaintext", 1, "hello\nworld"));
		tracker.didOpen(openParams);
		DidChangeTextDocumentParams changeParams = new DidChangeTextDocumentParams();
		changeParams.setTextDocument(new VersionedTextDocumentIdentifier("file.txt", 2));
		TextDocumentContentChangeEvent changeEvent = new TextDocumentContentChangeEvent();
		changeEvent.setText("affles");
		changeEvent.setRange(new Range(new Position(1, 1), new Position(1, 5)));
		changeEvent.setRangeLength(4);
		changeParams.setContentChanges(Collections.singletonList(changeEvent));
		tracker.didChange(changeParams);
		Assertions.assertEquals("hello\nwaffles", tracker.getContents(URI.create("file.txt")));
	}
}