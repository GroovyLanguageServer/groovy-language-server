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

class GroovyServicesDefinitionTests {
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
	void testLocalVariableDefinitionFromDeclaration() throws Exception {
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
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
		List<? extends Location> locations = services.definition(new TextDocumentPositionParams(textDocument, position))
				.get();
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
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
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
		List<? extends Location> locations = services.definition(new TextDocumentPositionParams(textDocument, position))
				.get();
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
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
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
		List<? extends Location> locations = services.definition(new TextDocumentPositionParams(textDocument, position))
				.get();
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
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
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
		List<? extends Location> locations = services.definition(new TextDocumentPositionParams(textDocument, position))
				.get();
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
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public int memberVar\n");
		contents.append("}\n");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(1, 18);
		List<? extends Location> locations = services.definition(new TextDocumentPositionParams(textDocument, position))
				.get();
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
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
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
		List<? extends Location> locations = services.definition(new TextDocumentPositionParams(textDocument, position))
				.get();
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
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("  public void memberMethod() {}\n");
		contents.append("}\n");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(1, 16);
		List<? extends Location> locations = services.definition(new TextDocumentPositionParams(textDocument, position))
				.get();
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
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
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
		List<? extends Location> locations = services.definition(new TextDocumentPositionParams(textDocument, position))
				.get();
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
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class Definitions {\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(0, 8);
		List<? extends Location> locations = services.definition(new TextDocumentPositionParams(textDocument, position))
				.get();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(0, location.getRange().getStart().getLine());
		Assertions.assertEquals(0, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(1, location.getRange().getEnd().getLine());
		Assertions.assertEquals(1, location.getRange().getEnd().getCharacter());
	}

	@Test
	void testClassDefinitionFromConstructorCall() throws Exception {
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
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
		List<? extends Location> locations = services.definition(new TextDocumentPositionParams(textDocument, position))
				.get();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		/*Assertions.assertEquals(0, location.getRange().getStart().getLine());
		Assertions.assertEquals(0, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(1, location.getRange().getEnd().getLine());
		Assertions.assertEquals(1, location.getRange().getEnd().getCharacter());*/
	}

	@Test
	void testClassDefinitionFromVariableDeclaration() throws Exception {
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
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
		List<? extends Location> locations = services.definition(new TextDocumentPositionParams(textDocument, position))
				.get();
		Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		/*Assertions.assertEquals(0, location.getRange().getStart().getLine());
		Assertions.assertEquals(0, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(1, location.getRange().getEnd().getLine());
		Assertions.assertEquals(1, location.getRange().getEnd().getCharacter());*/
	}

	@Test
	void testClassDefinitionFromClassExpression() throws Exception {
		Path filePath = workspaceRoot.resolve("./src/main/java/Definitions.groovy");
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
		List<? extends Location> locations = services.definition(new TextDocumentPositionParams(textDocument, position))
				.get();
		/*Assertions.assertEquals(1, locations.size());
		Location location = locations.get(0);
		Assertions.assertEquals(uri, location.getUri());
		Assertions.assertEquals(0, location.getRange().getStart().getLine());
		Assertions.assertEquals(0, location.getRange().getStart().getCharacter());
		Assertions.assertEquals(1, location.getRange().getEnd().getLine());
		Assertions.assertEquals(1, location.getRange().getEnd().getCharacter());*/
	}
}