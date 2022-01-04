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
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Location;
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

class GroovyServicesDefinitionTests {
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

	// --- local variables

	@Test
	void testLocalVariableDefinitionFromDeclaration() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public Definitions() {\n");
		contents.append("    int localVar\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(2, 14);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(2, location.getRange().getStart().getLine());
		Assertions.assertEquals(8, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(2, location.getRange().getEnd().getLine());
		Assertions.assertEquals(16, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testLocalVariableDefinitionFromAssignment() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public Definitions() {\n");
		contents.append("    int localVar\n");
		contents.append("    localVar = 123\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 6);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(2, location.getRange().getStart().getLine());
		Assertions.assertEquals(8, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(2, location.getRange().getEnd().getLine());
		Assertions.assertEquals(16, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testLocalVariableDefinitionFromMethodCallObjectExpression() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public Definitions() {\n");
		contents.append("    String localVar = \"hi\"\n");
		contents.append("    localVar.charAt(0)\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 6);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(2, location.getRange().getStart().getLine());
		Assertions.assertEquals(11, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(2, location.getRange().getEnd().getLine());
		Assertions.assertEquals(19, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testLocalVariableDefinitionFromMethodCallArgument() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public Definitions() {\n");
		contents.append("    int localVar\n");
		contents.append("    this.method(localVar)\n");
		contents.append("  }\n");
		contents.append("  public void method(int param) {}\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 18);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(2, location.getRange().getStart().getLine());
		Assertions.assertEquals(8, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(2, location.getRange().getEnd().getLine());
		Assertions.assertEquals(16, location.getRange().getEnd().getCharacter());
	}

	// --- member variables

	@Test
	void testMemberVariableDefinitionFromDeclaration() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public int memberVar\n");
		contents.append("}\n");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(1, 18);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(1, location.getRange().getStart().getLine());
		Assertions.assertEquals(2, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(1, location.getRange().getEnd().getLine());
		Assertions.assertEquals(22, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testMemberVariableDefinitionFromAssignment() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public int memberVar\n");
		contents.append("  public Definitions() {\n");
		contents.append("    memberVar = 123\n");
		contents.append("  }\n");
		contents.append("}\n");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 6);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(1, location.getRange().getStart().getLine());
		Assertions.assertEquals(2, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(1, location.getRange().getEnd().getLine());
		Assertions.assertEquals(22, location.getRange().getEnd().getCharacter());
	}

	// --- member methods

	@Test
	void testMemberMethodDefinitionFromDeclaration() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public void memberMethod() {}\n");
		contents.append("}\n");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(1, 16);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(1, location.getRange().getStart().getLine());
		Assertions.assertEquals(2, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(1, location.getRange().getEnd().getLine());
		Assertions.assertEquals(31, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testMemberMethodDefinitionFromCall() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public void memberMethod() {}\n");
		contents.append("  public Definitions() {\n");
		contents.append("    memberMethod()\n");
		contents.append("  }\n");
		contents.append("}\n");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 6);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(1, location.getRange().getStart().getLine());
		Assertions.assertEquals(2, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(1, location.getRange().getEnd().getLine());
		Assertions.assertEquals(31, location.getRange().getEnd().getCharacter());
	}

	// --- classes

	@Test
	void testClassDefinitionFromDeclaration() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(0, 8);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(0, location.getRange().getStart().getLine());
		Assertions.assertEquals(0, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(1, location.getRange().getEnd().getLine());
		Assertions.assertEquals(1, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testConstructorDefinitionFromConstructorCall() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public Definitions() {\n");
		contents.append("    new Definitions()\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(2, 10);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(1, location.getRange().getStart().getLine());
		Assertions.assertEquals(2, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(3, location.getRange().getEnd().getLine());
		Assertions.assertEquals(3, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testClassDefinitionFromVariableDeclaration() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public Definitions() {\n");
		contents.append("    Definitions d\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(2, 6);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(0, location.getRange().getStart().getLine());
		Assertions.assertEquals(0, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(4, location.getRange().getEnd().getLine());
		Assertions.assertEquals(1, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testClassDefinitionFromClassExpression() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public static void staticMethod() {}\n");
		contents.append("  public Definitions() {\n");
		contents.append("    Definitions.staticMethod()\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 6);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(0, location.getRange().getStart().getLine());
		Assertions.assertEquals(0, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(5, location.getRange().getEnd().getLine());
		Assertions.assertEquals(1, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testClassDefinitionFromImport() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));

		Path filePath2 = srcRoot.resolve("Definitions2.groovy");
		String uri2 = filePath2.toUri().toString();
		StringBuilder contents2 = new StringBuilder();
		contents2.append("import Definitions\n");
		contents2.append("class Definitions2 {\n");
		contents2.append("}");
		TextDocumentItem textDocumentItem2 = new TextDocumentItem(uri2, LANGUAGE_GROOVY, 1, contents2.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem2));

		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri2);
		Position position = new Position(0, 10);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(0, location.getRange().getStart().getLine());
		Assertions.assertEquals(0, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(1, location.getRange().getEnd().getLine());
		Assertions.assertEquals(1, location.getRange().getEnd().getCharacter());
	}

	// --- parameters

	@Test
	void testParameterDefinitionFromDeclarationInConstructor() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public Definitions(int param) {\n");
		contents.append("  }\n");
		contents.append("}\n");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(1, 27);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(1, location.getRange().getStart().getLine());
		Assertions.assertEquals(21, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(1, location.getRange().getEnd().getLine());
		Assertions.assertEquals(30, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testParameterDefinitionFromDeclarationInMethod() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public void memberMethod(int param) {\n");
		contents.append("  }\n");
		contents.append("}\n");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(1, 33);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(1, location.getRange().getStart().getLine());
		Assertions.assertEquals(27, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(1, location.getRange().getEnd().getLine());
		Assertions.assertEquals(36, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testParameterDefinitionFromReference() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public void memberMethod(int param) {\n");
		contents.append("    param\n");
		contents.append("  }\n");
		contents.append("}\n");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(2, 6);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(1, location.getRange().getStart().getLine());
		Assertions.assertEquals(27, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(1, location.getRange().getEnd().getLine());
		Assertions.assertEquals(36, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testDefinitionFromArrayItemMemberAccess() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public Definitions() {\n");
		contents.append("    Definitions[] items\n");
		contents.append("    items[0].hello\n");
		contents.append("  }\n");
		contents.append("  public String hello\n");
		contents.append("}\n");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 15);
		List<? extends Location> locations = services.definition(new DefinitionParams(textDocument, position)).get()
				.getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(5, location.getRange().getStart().getLine());
		Assertions.assertEquals(2, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(5, location.getRange().getEnd().getLine());
		Assertions.assertEquals(21, location.getRange().getEnd().getCharacter());
	}
}