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

import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TypeDefinitionParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.prominic.groovyls.config.CompilationUnitFactory;

class GroovyServicesTypeDefinitionTests {
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
	void testLocalVariableTypeDefinitionFromDeclaration() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class TypeDefinitions {\n");
		contents.append("  public TypeDefinitions() {\n");
		contents.append("    TypeDefinitions localVar\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(2, 22);
		List<? extends Location> locations = services.typeDefinition(new TypeDefinitionParams(textDocument, position))
				.get().getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(0, location.getRange().getStart().getLine());
		Assertions.assertEquals(0, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(4, location.getRange().getEnd().getLine());
		Assertions.assertEquals(1, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testLocalVariableTypeDefinitionFromAssignment() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class TypeDefinitions {\n");
		contents.append("  public TypeDefinitions() {\n");
		contents.append("    TypeDefinitions localVar\n");
		contents.append("    localVar = null\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 6);
		List<? extends Location> locations = services.typeDefinition(new TypeDefinitionParams(textDocument, position))
				.get().getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(0, location.getRange().getStart().getLine());
		Assertions.assertEquals(0, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(5, location.getRange().getEnd().getLine());
		Assertions.assertEquals(1, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testLocalVariableTypeDefinitionFromMethodCall() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class TypeDefinitions {\n");
		contents.append("  public void method() {\n");
		contents.append("  }\n");
		contents.append("  public TypeDefinitions() {\n");
		contents.append("    TypeDefinitions localVar\n");
		contents.append("    localVar.method()\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(5, 6);
		List<? extends Location> locations = services.typeDefinition(new TypeDefinitionParams(textDocument, position))
				.get().getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(0, location.getRange().getStart().getLine());
		Assertions.assertEquals(0, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(7, location.getRange().getEnd().getLine());
		Assertions.assertEquals(1, location.getRange().getEnd().getCharacter());
	}

	// --- member variables

	@Test
	void testMemberVariableTypeDefinitionFromDeclaration() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class TypeDefinitions {\n");
		contents.append("  TypeDefinitions memberVar\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(1, 20);
		List<? extends Location> locations = services.typeDefinition(new TypeDefinitionParams(textDocument, position))
				.get().getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(0, location.getRange().getStart().getLine());
		Assertions.assertEquals(0, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(2, location.getRange().getEnd().getLine());
		Assertions.assertEquals(1, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testMemberVariableTypeDefinitionFromAssignment() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class TypeDefinitions {\n");
		contents.append("  TypeDefinitions memberVar\n");
		contents.append("  public TypeDefinitions() {\n");
		contents.append("    memberVar = null\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 6);
		List<? extends Location> locations = services.typeDefinition(new TypeDefinitionParams(textDocument, position))
				.get().getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(0, location.getRange().getStart().getLine());
		Assertions.assertEquals(0, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(5, location.getRange().getEnd().getLine());
		Assertions.assertEquals(1, location.getRange().getEnd().getCharacter());
	}

	// --- member methods

	@Test
	void testMemberMethodTypeDefinitionFromDeclaration() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class TypeDefinitions {\n");
		contents.append("  public TypeDefinitions memberMethod() {\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(1, 27);
		List<? extends Location> locations = services.typeDefinition(new TypeDefinitionParams(textDocument, position))
				.get().getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(0, location.getRange().getStart().getLine());
		Assertions.assertEquals(0, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(3, location.getRange().getEnd().getLine());
		Assertions.assertEquals(1, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testMemberMethodTypeDefinitionFromCall() throws Exception {
		Path filePath = srcRoot.resolve("Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class TypeDefinitions {\n");
		contents.append("  public TypeDefinitions memberMethod() {\n");
		contents.append("  }\n");
		contents.append("  public TypeDefinitions() {\n");
		contents.append("    memberMethod()\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(4, 6);
		List<? extends Location> locations = services.typeDefinition(new TypeDefinitionParams(textDocument, position))
				.get().getLeft();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(0, location.getRange().getStart().getLine());
		Assertions.assertEquals(0, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(6, location.getRange().getEnd().getLine());
		Assertions.assertEquals(1, location.getRange().getEnd().getCharacter());
	}
}