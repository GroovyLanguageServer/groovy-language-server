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

import net.prominic.groovyls.config.CompilationUnitFactory;
import net.prominic.groovyls.config.ICompilationUnitFactory;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.*;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class GroovyLanguageServer implements LanguageServer, LanguageClientAware {

    private static final Logger logger = LoggerFactory.getLogger(GroovyLanguageServer.class);
    private LanguageClient client;

    public static void main(String[] args) throws IOException {
        if (args.length > 0 && "--tcp".equals(args[0])) {
            int port = 5007;
            if (args.length > 1) {
                try {
                    port = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    logger.error("Invalid port number: {}", args[1]);
                    System.exit(1);
                }
            }

            try (ServerSocket serverSocket = new ServerSocket(port)) {
                logger.info("Groovy Language Server listening on port {}", port);
                Socket socket = serverSocket.accept();
                logger.info("Client connected.");

                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                startServer(in, out);
            }
        } else {
            logger.info("Groovy Language Server starting in stdio mode.");
            InputStream in = System.in;
            OutputStream out = System.out;
            startServer(in, out);
        }
    }

    private static void startServer(InputStream in, OutputStream out) {
        // Redirect System.out to System.err to avoid corrupting the communication channel
        System.setOut(new PrintStream(System.err));

        GroovyLanguageServer server = new GroovyLanguageServer();
        Launcher<LanguageClient> launcher = Launcher.createLauncher(server, LanguageClient.class, in, out);
        server.connect(launcher.getRemoteProxy());
        launcher.startListening();
    }

    private final GroovyServices groovyServices;

    public GroovyLanguageServer() {
        this(new CompilationUnitFactory());
    }

    public GroovyLanguageServer(ICompilationUnitFactory compilationUnitFactory) {
        this.groovyServices = new GroovyServices(compilationUnitFactory);
    }

    private List<Path> discoverGradleProjects(Path root) throws IOException {
        List<Path> gradleProjects = new ArrayList<>();
        try (Stream<Path> fileStream = Files.walk(root)) {
            fileStream
                    .filter(Files::isRegularFile)
                    .filter(p -> Set.of("build.gradle", "build.gradle.kts").contains(p.getFileName().toString()))
                    .forEach(buildFile -> gradleProjects.add(buildFile.getParent()));
        }
        return gradleProjects;
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        String rootUriString = params.getRootUri();
        if (rootUriString != null) {
            URI uri = URI.create(rootUriString);
            Path workspaceRoot = Paths.get(uri);
            groovyServices.setWorkspaceRoot(workspaceRoot);
        }

        List<WorkspaceFolder> folders = params.getWorkspaceFolders();
        if (folders != null) {
            for (WorkspaceFolder folder : folders) {
                Path folderPath = Paths.get(URI.create(folder.getUri()));
                try {
                    List<Path> gradleProjects = discoverGradleProjects(folderPath);
                    for (Path gradleProject : gradleProjects) {
                        if (client != null) {
                            client.showMessage(new MessageParams(
                                    MessageType.Info,
                                    "Importing Gradle project: " + gradleProject
                            ));
                        }
                        importGradleProject(gradleProject);
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        CompletionOptions completionOptions = new CompletionOptions(false, Arrays.asList("."));
        ServerCapabilities serverCapabilities = new ServerCapabilities();
        serverCapabilities.setCompletionProvider(completionOptions);
        serverCapabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        serverCapabilities.setDocumentSymbolProvider(true);
        serverCapabilities.setWorkspaceSymbolProvider(true);
        serverCapabilities.setDocumentSymbolProvider(true);
        serverCapabilities.setReferencesProvider(true);
        serverCapabilities.setDefinitionProvider(true);
        serverCapabilities.setTypeDefinitionProvider(true);
        serverCapabilities.setHoverProvider(true);
        serverCapabilities.setRenameProvider(true);
        SignatureHelpOptions signatureHelpOptions = new SignatureHelpOptions();
        signatureHelpOptions.setTriggerCharacters(Arrays.asList("(", ","));
        serverCapabilities.setSignatureHelpProvider(signatureHelpOptions);

        InitializeResult initializeResult = new InitializeResult(serverCapabilities);
        return CompletableFuture.completedFuture(initializeResult);
    }

    public void importGradleProject(Path folderPath) {
        GradleConnector connector = GradleConnector.newConnector()
                .forProjectDirectory(folderPath.toFile());

        try (ProjectConnection connection = connector.connect()) {
            // First run the build (blocking)
            connection.newBuild()
                    .forTasks("classes")
                    .setStandardOutput(System.out)
                    .setStandardError(System.err)
                    .run();

            IdeaProject project = connection.getModel(IdeaProject.class);

            List<String> classpathList = new ArrayList<>();

            for (IdeaModule module : project.getChildren()) {
                // Compiler output dirs
                if (module.getCompilerOutput() != null) {
                    File outputDir = module.getCompilerOutput().getOutputDir();
                    if (outputDir != null) {
                        classpathList.add(outputDir.getAbsolutePath());
                    }
                }

                classpathList.addAll(discoverClassDirs(folderPath));

                for (IdeaDependency dep : module.getDependencies()) {
                    if (dep instanceof IdeaSingleEntryLibraryDependency) {
                        File file = ((IdeaSingleEntryLibraryDependency) dep).getFile();
                        if (file != null && file.exists()) {
                            classpathList.add(file.getAbsolutePath());
                        }
                    }
                }
            }

            logger.info("classpathList: {}", classpathList);
            groovyServices.updateClasspath(classpathList);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private List<String> discoverClassDirs(Path projectDir) throws IOException {
        Path classesRoot = projectDir.resolve("build/classes");
        List<String> classDirs = new ArrayList<>();

        if (Files.exists(classesRoot)) {
            try (Stream<Path> stream = Files.walk(classesRoot, 2)) {
                stream
                        .filter(Files::isDirectory)
                        .map(Path::toString)
                        .forEach(classDirs::add);
            }
        }
        return classDirs;
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(new Object());
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return groovyServices;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return groovyServices;
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
        groovyServices.connect(client);
    }
}
