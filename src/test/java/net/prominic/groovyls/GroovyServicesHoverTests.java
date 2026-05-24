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
package net.prominic.groovyls;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.prominic.groovyls.config.CompilationUnitFactory;

class GroovyServicesHoverTests {
	private static final String LANGUAGE_GROOVY = "groovy";
	private static final String PATH_WORKSPACE = "./build/test_workspace/";
	private static final String PATH_SRC = "./src/main/groovy";

	private GroovyServices services;
	private Path workspaceRoot;
	private Path srcRoot;

	@BeforeEach
	void setup() {
		workspaceRoot = Paths.get(System.getProperty("user.dir")).resolve(PATH_WORKSPACE);
		srcRoot = workspaceRoot.resolve(PATH_SRC);
		if (!Files.exists(srcRoot)) {
			srcRoot.toFile().mkdirs();
		}

		services = new GroovyServices(new CompilationUnitFactory());
		services.setWorkspaceRoot(workspaceRoot);
		services.connect(new LanguageClient() {

			@Override
			public void telemetryEvent(Object object) {

			}

			@Override
			public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
				return null;
			}

			@Override
			public void showMessage(MessageParams messageParams) {

			}

			@Override
			public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {

			}

			@Override
			public void logMessage(MessageParams message) {

			}
		});
	}

	@AfterEach
	void tearDown() {
		services = null;
		workspaceRoot = null;
		srcRoot = null;
	}

	@Test
	void testHoverOnForEachMethod() throws Exception {
		Path filePath = srcRoot.resolve("Hover.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class HoverTest {\n");
		contents.append("  public void test() {\n");
		contents.append("    [].forEach({ item ->\n");
		contents.append("      println(item)\n");
		contents.append("    })\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		// Position is at the 'f' in forEach on line 2 (0-indexed)
		Position position = new Position(2, 9);
		Hover hover = services.hover(new HoverParams(textDocument, position)).get();
		
		Assertions.assertNotNull(hover, "Hover should not be null");
		Assertions.assertNotNull(hover.getContents(), "Hover contents should not be null");
		String hoverContent = hover.getContents().getRight().getValue();
		Assertions.assertNotNull(hoverContent, "Hover content value should not be null");
		Assertions.assertTrue(hoverContent.contains("forEach"), "Hover should contain 'forEach' method name");
	}

	@Test
	void testHoverOnMethodDeclaration() throws Exception {
		Path filePath = srcRoot.resolve("Hover.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class HoverTest {\n");
		contents.append("  public void myMethod(int param) {\n");
		contents.append("    println(param)\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		// Position is at the 'm' in myMethod
		Position position = new Position(1, 16);
		Hover hover = services.hover(new HoverParams(textDocument, position)).get();
		
		Assertions.assertNotNull(hover, "Hover should not be null");
		Assertions.assertNotNull(hover.getContents(), "Hover contents should not be null");
		String hoverContent = hover.getContents().getRight().getValue();
		Assertions.assertNotNull(hoverContent, "Hover content value should not be null");
		Assertions.assertTrue(hoverContent.contains("myMethod"), "Hover should contain method name");
		Assertions.assertTrue(hoverContent.contains("int param"), "Hover should contain parameter type");
	}

	@Test
	void testHoverOnClassDeclaration() throws Exception {
		Path filePath = srcRoot.resolve("Hover.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class MyClass {\n");
		contents.append("  public void test() {}\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		// Position is at 'M' in MyClass
		Position position = new Position(0, 6);
		Hover hover = services.hover(new HoverParams(textDocument, position)).get();
		
		Assertions.assertNotNull(hover, "Hover should not be null");
		Assertions.assertNotNull(hover.getContents(), "Hover contents should not be null");
		String hoverContent = hover.getContents().getRight().getValue();
		Assertions.assertNotNull(hoverContent, "Hover content value should not be null");
		Assertions.assertTrue(hoverContent.contains("MyClass"), "Hover should contain class name");
	}

	@Test
	void testHoverOnVariable() throws Exception {
		Path filePath = srcRoot.resolve("Hover.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class HoverTest {\n");
		contents.append("  public void test() {\n");
		contents.append("    String message = \"hello\"\n");
		contents.append("    println(message)\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		// Position is at 'm' in message (in the println call)
		Position position = new Position(3, 13);
		Hover hover = services.hover(new HoverParams(textDocument, position)).get();
		
		Assertions.assertNotNull(hover, "Hover should not be null");
		Assertions.assertNotNull(hover.getContents(), "Hover contents should not be null");
		String hoverContent = hover.getContents().getRight().getValue();
		Assertions.assertNotNull(hoverContent, "Hover content value should not be null");
		Assertions.assertTrue(hoverContent.contains("message"), "Hover should contain variable name");
		Assertions.assertTrue(hoverContent.contains("String"), "Hover should contain variable type");
	}

	@Test
	void testHoverOnNonExistentSymbol() throws Exception {
		Path filePath = srcRoot.resolve("Hover.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class HoverTest {\n");
		contents.append("  public void test() {\n");
		contents.append("    // Just a comment\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		// Position is in a comment
		Position position = new Position(2, 10);
		Hover hover = services.hover(new HoverParams(textDocument, position)).get();
		
		// Hovering on a comment should return null
		Assertions.assertNull(hover, "Hover should be null for comments");
	}
}
