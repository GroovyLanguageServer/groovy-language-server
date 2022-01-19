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
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.prominic.groovyls.config.CompilationUnitFactory;

class GroovyServicesCompletionTests {
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
	void testMemberAccessOnLocalVariableAfterDot() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  public Completion() {\n");
		contents.append("    String localVar\n");
		contents.append("    localVar.\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 13);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		Assertions.assertTrue(items.size() > 0);
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("charAt") && item.getKind().equals(CompletionItemKind.Method);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testMemberAccessOnMemberVariableAfterDot() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  String memberVar\n");
		contents.append("  public Completion() {\n");
		contents.append("    memberVar.\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 14);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		Assertions.assertTrue(items.size() > 0);
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("charAt") && item.getKind().equals(CompletionItemKind.Method);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testMemberAccessOnThisAfterDot() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  String memberVar\n");
		contents.append("  public Completion() {\n");
		contents.append("    this.\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 9);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		Assertions.assertTrue(items.size() > 0);
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("memberVar") && item.getKind().equals(CompletionItemKind.Field);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testMemberAccessOnClassAfterDot() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  public Completion() {\n");
		contents.append("    Completion.\n");
		contents.append("  }\n");
		contents.append("  public static void staticMethod() {}\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(2, 15);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		Assertions.assertTrue(items.size() > 0);
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("staticMethod") && item.getKind().equals(CompletionItemKind.Method);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testMemberAccessOnLocalArrayAfterDot() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  public Completion() {\n");
		contents.append("    String[] localVar\n");
		contents.append("    localVar[0].\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 16);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		Assertions.assertTrue(items.size() > 0);
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("charAt") && item.getKind().equals(CompletionItemKind.Method);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testMemberAccessOnLocalVariableWithPartialPropertyExpression() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  public Completion() {\n");
		contents.append("    String localVar\n");
		contents.append("    localVar.charA\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 18);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("charAt") && item.getKind().equals(CompletionItemKind.Method);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testMemberAccessOnThisWithMultipleResults() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  public Completion() {\n");
		contents.append("    this.abcde\n");
		contents.append("  }\n");
		contents.append("  public abc() {}\n");
		contents.append("  public abcdef() {}\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));

		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);

		// this first test should include both methods...
		Position position = new Position(2, 11);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		Assertions.assertEquals(2, items.size());
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return (item.getLabel().equals("abc") && item.getKind().equals(CompletionItemKind.Method))
					|| (item.getLabel().equals("abcdef") && item.getKind().equals(CompletionItemKind.Method));
		}).collect(Collectors.toList());
		Assertions.assertEquals(2, filteredItems.size());

		// ...and this one should only include the one with the longer name
		position = new Position(2, 13);
		result = services.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		items = result.getLeft();
		Assertions.assertEquals(1, items.size());
		CompletionItem item = items.get(0);
		Assertions.assertEquals("abcdef", item.getLabel());
		Assertions.assertEquals(CompletionItemKind.Method, item.getKind());
	}

	@Test
	void testMemberAccessOnLocalVariableWithExistingVariableExpressionOnNextLine() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  public Completion() {\n");
		contents.append("    String localVar\n");
		contents.append("    localVar.\n");
		contents.append("    localVar\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 13);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		Assertions.assertTrue(items.size() > 0);
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("charAt") && item.getKind().equals(CompletionItemKind.Method);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testMemberAccessOnLocalVariableWithExistingMethodCallExpressionOnNextLine() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  public Completion() {\n");
		contents.append("    String localVar\n");
		contents.append("    localVar.\n");
		contents.append("    method()\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 13);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		Assertions.assertTrue(items.size() > 0);
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("charAt") && item.getKind().equals(CompletionItemKind.Method);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testCompletionForMemberVariableOnPartialVariableExpression() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  String memberVar\n");
		contents.append("  public Completion() {\n");
		contents.append("    mem\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 7);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("memberVar") && item.getKind().equals(CompletionItemKind.Field);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testCompletionForMemberVariableOnCompleteVariableExpression() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  String memberVar\n");
		contents.append("  public Completion() {\n");
		contents.append("    memberVar\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 7);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("memberVar") && item.getKind().equals(CompletionItemKind.Field);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testCompletionForMemberMethodOnPartialVariableExpression() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  String memberMethod() {}\n");
		contents.append("  public Completion() {\n");
		contents.append("    mem\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 7);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("memberMethod") && item.getKind().equals(CompletionItemKind.Method);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testCompletionForMemberMethodOnCompleteVariableExpression() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  String memberMethod() {}\n");
		contents.append("  public Completion() {\n");
		contents.append("    memberMethod\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 7);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("memberMethod") && item.getKind().equals(CompletionItemKind.Method);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testCompletionForParameterOnPartialVariableExpression() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  public void testMethod(String paramName) {\n");
		contents.append("    par\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(2, 7);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("paramName") && item.getKind().equals(CompletionItemKind.Variable);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testCompletionForParameterOnCompleteVariableExpression() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  public void testMethod(String paramName) {\n");
		contents.append("    paramName\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(2, 13);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("paramName") && item.getKind().equals(CompletionItemKind.Variable);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testCompletionForLocalVariableOnPartialVariableExpression() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  public void testMethod(String paramName) {\n");
		contents.append("    String localVar\n");
		contents.append("    loc\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 7);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("localVar") && item.getKind().equals(CompletionItemKind.Variable);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testCompletionForLocalVariableOnCompleteVariableExpression() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  public void testMethod() {\n");
		contents.append("    String localVar\n");
		contents.append("    localVar\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 12);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("localVar") && item.getKind().equals(CompletionItemKind.Variable);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testCompletionForLocalVariableOnPartialVariableExpressionInsideBlock() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  public void testMethod(String paramName) {\n");
		contents.append("    String localVar\n");
		contents.append("    if(true) {\n");
		contents.append("      loc\n");
		contents.append("    }\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(4, 9);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("localVar") && item.getKind().equals(CompletionItemKind.Variable);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testOwnClass() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("package com.example;\n");
		contents.append("class Completion {\n");
		contents.append("  public Completion() {\n");
		contents.append("    Completio\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(3, 13);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		Assertions.assertTrue(items.size() > 0);
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("Completion") && item.getDetail().equals("com.example")
					&& item.getKind().equals(CompletionItemKind.Class);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}

	@Test
	void testSystemClass() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Completion {\n");
		contents.append("  public Completion() {\n");
		contents.append("    ArrayLis\n");
		contents.append("  }\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(2, 12);
		Either<List<CompletionItem>, CompletionList> result = services
				.completion(new CompletionParams(textDocument, position)).get();
		Assertions.assertTrue(result.isLeft());
		List<CompletionItem> items = result.getLeft();
		Assertions.assertTrue(items.size() > 0);
		List<CompletionItem> filteredItems = items.stream().filter(item -> {
			return item.getLabel().equals("ArrayList") && item.getDetail().equals("java.util")
					&& item.getKind().equals(CompletionItemKind.Class);
		}).collect(Collectors.toList());
		Assertions.assertEquals(1, filteredItems.size());
	}
}