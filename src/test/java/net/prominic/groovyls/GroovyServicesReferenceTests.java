package net.prominic.groovyls;

import net.prominic.groovyls.config.CompilationUnitFactory;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GroovyServicesReferenceTests {
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
    void getUsagesOfMethodFromClass() throws Exception{
        List<TextDocumentItem> textDocumentItem = getTextDocumentForUsage(srcRoot);
        services.didOpen(new DidOpenTextDocumentParams(textDocumentItem.get(0)));
        services.didOpen(new DidOpenTextDocumentParams(textDocumentItem.get(1)));
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier(textDocumentItem.get(0).getUri());
        Position position = new Position(1, 15);
        List<? extends Location> locations = services.references(new ReferenceParams(textDocument, position,new ReferenceContext(true))).get();
        Assertions.assertEquals(3, locations.size());
        List<Location> locationList = locations.stream().filter(r->r.getUri().equals(textDocumentItem.get(1).getUri())).collect(Collectors.toList());
        Location location = locationList.get(0);
        Assertions.assertEquals(textDocumentItem.get(1).getUri(), location.getUri());
        Assertions.assertEquals(7, location.getRange().getStart().getLine());
        Assertions.assertEquals(59, location.getRange().getStart().getCharacter());
        Assertions.assertEquals(7, location.getRange().getEnd().getLine());
        Assertions.assertEquals(76, location.getRange().getEnd().getCharacter());
        Location location2 = locationList.get(1);
        Assertions.assertEquals(textDocumentItem.get(1).getUri(), location2.getUri());
        Assertions.assertEquals(11, location2.getRange().getStart().getLine());
        Assertions.assertEquals(21, location2.getRange().getStart().getCharacter());
        Assertions.assertEquals(11, location2.getRange().getEnd().getLine());
        Assertions.assertEquals(38, location2.getRange().getEnd().getCharacter());
    }

    private static List<TextDocumentItem> getTextDocumentForUsage(Path srcRoot) {

        Path filePath = srcRoot.resolve("MyClass.groovy");
        String uri = filePath.toUri().toString();
        StringBuilder contents = new StringBuilder();

        contents.append("class MyClass{\n");
        contents.append("   String getMyClassVersion(){\n");
        contents.append("       return \"1.0.0\";\n");
        contents.append("   }\n");
        contents.append("}\n");

        TextDocumentItem textDocumentMyClass = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());

        Path userfilePath = srcRoot.resolve("User.groovy");
        String userfileuri = userfilePath.toUri().toString();
        StringBuilder userFileContents = new StringBuilder();

        userFileContents.append("class User{\n");
        userFileContents.append("   MyClass myclass;\n");
        userFileContents.append("\n");
        userFileContents.append("   User(MyClass ref){\n");
        userFileContents.append("     myclass = ref;\n");
        userFileContents.append("   }\n");
        userFileContents.append("   void doStuff(){\n");
        userFileContents.append("      String out = \"The version of my class is \" + myclass.getMyClassVersion();\n");
        userFileContents.append("    }\n");
        userFileContents.append("\n");
        userFileContents.append("   String getLocalClassVersion() {\n");
        userFileContents.append("      return myclass.getMyClassVersion();\n");
        userFileContents.append("  }\n");
        userFileContents.append("}\n");

        TextDocumentItem textDocumentUser = new TextDocumentItem(userfileuri, LANGUAGE_GROOVY, 1, userFileContents.toString());

        return Arrays.asList(textDocumentMyClass,textDocumentUser);
    }


    @Test
    void getUsagesOfMethodFromClassObjectDeclaration() throws Exception{
        List<TextDocumentItem> textDocumentItems = getTextDocumentForUsageObjectDeclaration(srcRoot);

        services.didOpen(new DidOpenTextDocumentParams(textDocumentItems.get(0)));
        services.didOpen(new DidOpenTextDocumentParams(textDocumentItems.get(1)));
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier(textDocumentItems.get(0).getUri());
        Position position = new Position(1, 15);
        List<? extends Location> locations = services.references(new ReferenceParams(textDocument, position,new ReferenceContext(true))).get();
        Assertions.assertEquals(2, locations.size());
        Optional<? extends Location> location = locations.stream().filter(it-> it.getUri().equals(textDocumentItems.get(1).getUri())).findFirst();
        Assertions.assertTrue(location.isPresent());
        Assertions.assertEquals(textDocumentItems.get(1).getUri(), location.get().getUri());
        Assertions.assertEquals(3, location.get().getRange().getStart().getLine());
        Assertions.assertEquals(9, location.get().getRange().getStart().getCharacter());
        Assertions.assertEquals(3, location.get().getRange().getEnd().getLine());
        Assertions.assertEquals(26, location.get().getRange().getEnd().getCharacter());
    }
    private static List<TextDocumentItem> getTextDocumentForUsageObjectDeclaration(Path srcRoot){
        Path filePath = srcRoot.resolve("MyClass.groovy");
        String uri = filePath.toUri().toString();
        StringBuilder contents = new StringBuilder();

        contents.append("class MyClass{\n");
        contents.append("   String getMyClassVersion(){\n");
        contents.append("       return \"1.0.0\";\n");
        contents.append("   }\n");
        contents.append("}\n");
        TextDocumentItem textDocumentMyClass = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());

        Path userfilePath = srcRoot.resolve("User.groovy");
        String userfileuri = userfilePath.toUri().toString();
        StringBuilder userFileContents = new StringBuilder();

        userFileContents.append("class User{\n");
        userFileContents.append("   void doStuff(){\n");
        userFileContents.append("      MyClass mc = new MyClass();\n");
        userFileContents.append("      mc.getMyClassVersion();\n");
        userFileContents.append("    }\n");
        userFileContents.append("}\n");
        TextDocumentItem textDocumentUser = new TextDocumentItem(userfileuri, LANGUAGE_GROOVY, 1, userFileContents.toString());

        return Arrays.asList(textDocumentMyClass,textDocumentUser);

    }


    @Test
    void getUsagesOfVariableFromClassDeclaration() throws Exception{
        List<TextDocumentItem> textDocumentItems = getTextDocumentForUsageVariable(srcRoot);

        services.didOpen(new DidOpenTextDocumentParams(textDocumentItems.get(0)));
        services.didOpen(new DidOpenTextDocumentParams(textDocumentItems.get(1)));
        TextDocumentIdentifier textDocument = new TextDocumentIdentifier(textDocumentItems.get(0).getUri());
        Position position = new Position(1, 13);
        List<? extends Location> locations = services.references(new ReferenceParams(textDocument, position,new ReferenceContext(true))).get();
        Assertions.assertEquals(3, locations.size());
        List<? extends Location> locationFiltered = locations.stream().filter(it->it.getUri().equals(textDocumentItems.get(1).getUri())).collect(Collectors.toList());

        Assertions.assertEquals(locationFiltered.size(),2);
        Location location1 = locationFiltered.get(0);
        Assertions.assertEquals(3, location1.getRange().getStart().getLine());
        Assertions.assertEquals(26, location1.getRange().getStart().getCharacter());
        Assertions.assertEquals(3, location1.getRange().getEnd().getLine());
        Assertions.assertEquals(33, location1.getRange().getEnd().getCharacter());

        Location location2 = locationFiltered.get(1);
        Assertions.assertEquals(4, location2.getRange().getStart().getLine());
        Assertions.assertEquals(48, location2.getRange().getStart().getCharacter());
        Assertions.assertEquals(4, location2.getRange().getEnd().getLine());
        Assertions.assertEquals(55, location2.getRange().getEnd().getCharacter());
    }
    private static List<TextDocumentItem> getTextDocumentForUsageVariable(Path srcRoot){
        Path filePath = srcRoot.resolve("MyClass.groovy");
        String uri = filePath.toUri().toString();
        StringBuilder contents = new StringBuilder();
        contents.append("class MyClass{\n");
        contents.append(" String version = \"1.0\";\n");
        contents.append("}\n");
        contents.append("\n");
        contents.append("\n");
        TextDocumentItem textDocumentMyClass = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());

        Path userfilePath = srcRoot.resolve("User.groovy");
        String userfileuri = userfilePath.toUri().toString();
        StringBuilder userFileContents = new StringBuilder();

        userFileContents.append("class User{\n");
        userFileContents.append("   void doStuff(){\n");
        userFileContents.append("      MyClass mc = new MyClass();\n");
        userFileContents.append("      String version = mc.version;\n");
        userFileContents.append("      return \"The version of my class is \" + mc.version;\n");
        userFileContents.append("    }\n");
        userFileContents.append("}\n");
        TextDocumentItem textDocumentUser = new TextDocumentItem(userfileuri, LANGUAGE_GROOVY, 1, userFileContents.toString());

        return Arrays.asList(textDocumentMyClass,textDocumentUser);

    }
}
