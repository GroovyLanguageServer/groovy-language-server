package net.prominic.groovyls;

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
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroovyServicesTypeDefinitionTests {
	private static final String LANGUAGE_GROOVY = "groovy";

	private GroovyServices services;
	private Path workspaceRoot;

	@BeforeEach
	void setup() {
		workspaceRoot = Paths.get("./test_workspace");
		services = new GroovyServices();
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
	}

	// --- local variables

	@Test
	void testLocalVariableTypeDefinitionFromDeclaration() throws Exception {
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
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
		List<? extends Location> locations = services
				.typeDefinition(new TextDocumentPositionParams(textDocument, position)).get();
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
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
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
		List<? extends Location> locations = services
				.typeDefinition(new TextDocumentPositionParams(textDocument, position)).get();
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
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
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
		List<? extends Location> locations = services
				.typeDefinition(new TextDocumentPositionParams(textDocument, position)).get();
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
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class TypeDefinitions {\n");
		contents.append("  TypeDefinitions memberVar\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(1, 20);
		List<? extends Location> locations = services
				.typeDefinition(new TextDocumentPositionParams(textDocument, position)).get();
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
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
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
		List<? extends Location> locations = services
				.typeDefinition(new TextDocumentPositionParams(textDocument, position)).get();
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
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
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
		List<? extends Location> locations = services
				.typeDefinition(new TextDocumentPositionParams(textDocument, position)).get();
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
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
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
		List<? extends Location> locations = services
				.typeDefinition(new TextDocumentPositionParams(textDocument, position)).get();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(0, location.getRange().getStart().getLine());
		Assertions.assertEquals(0, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(6, location.getRange().getEnd().getLine());
		Assertions.assertEquals(1, location.getRange().getEnd().getCharacter());
	}
}