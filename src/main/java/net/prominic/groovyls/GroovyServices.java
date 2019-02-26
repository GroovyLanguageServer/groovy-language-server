////////////////////////////////////////////////////////////////////////////////
// Copyright 2019 Prominic.NET, Inc.
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.ErrorCollector;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import net.prominic.groovyls.compiler.ast.ASTNodeVisitor;
import net.prominic.groovyls.compiler.control.ErrorCollectorWithoutThrow;
import net.prominic.groovyls.compiler.control.GroovyCompilationUnit;
import net.prominic.groovyls.compiler.control.io.StringReaderSourceWithURI;
import net.prominic.groovyls.providers.DefinitionProvider;
import net.prominic.groovyls.providers.DocumentSymbolProvider;
import net.prominic.groovyls.providers.HoverProvider;
import net.prominic.groovyls.providers.ReferenceProvider;
import net.prominic.groovyls.providers.RenameProvider;
import net.prominic.groovyls.providers.WorkspaceSymbolProvider;
import net.prominic.groovyls.util.FileContentsTracker;
import net.prominic.groovyls.util.GroovyLanguageServerUtils;

public class GroovyServices implements TextDocumentService, WorkspaceService, LanguageClientAware {
	private static final String FILE_EXTENSION_GROOVY = ".groovy";
	private static final String FILE_EXTENSION_JAVA = ".java";

	private LanguageClient languageClient;

	private CompilerConfiguration compilerConfig;
	private GroovyCompilationUnit compilationUnit;
	private ASTNodeVisitor astVisitor;
	private Map<URI, List<Diagnostic>> prevDiagnosticsByFile;
	private Path tempDirectoryPath;
	@SuppressWarnings("unused")
	private Path workspaceRoot;
	private Path srcMainGroovyPath;
	private Path srcMainJavaPath;
	private Path srcTestGroovyPath;
	private Path srcTestJavaPath;
	private FileContentsTracker fileContentsTracker = new FileContentsTracker();

	public GroovyServices() {
		try {
			tempDirectoryPath = Files.createTempDirectory("groovylc");
		} catch (IOException e) {
			System.err.println("Failed to create temporary directory");
		}
		compilerConfig = new CompilerConfiguration();
		compilerConfig.setTargetDirectory(tempDirectoryPath.toFile());
	}

	public void setWorkspaceRoot(Path workspaceRoot) {
		this.workspaceRoot = workspaceRoot;
		srcMainGroovyPath = workspaceRoot.resolve("src/main/groovy");
		srcMainJavaPath = workspaceRoot.resolve("src/main/java");
		srcTestGroovyPath = workspaceRoot.resolve("src/test/groovy");
		srcTestJavaPath = workspaceRoot.resolve("src/test/java");
		createOrUpdateCompilationUnit();
	}

	@Override
	public void connect(LanguageClient client) {
		languageClient = client;
	}

	// --- NOTIFICATIONS

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		fileContentsTracker.didOpen(params);
		createOrUpdateCompilationUnit();

		URI uri = URI.create(params.getTextDocument().getUri());
		compile(Collections.singleton(uri));

		visitAST();
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		fileContentsTracker.didChange(params);
		createOrUpdateCompilationUnit();

		URI uri = URI.create(params.getTextDocument().getUri());
		compile(Collections.singleton(uri));

		visitAST();
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		fileContentsTracker.didClose(params);
		createOrUpdateCompilationUnit();

		URI uri = URI.create(params.getTextDocument().getUri());
		compile(Collections.singleton(uri));

		visitAST();
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		// nothing to handle on save at this time
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		createOrUpdateCompilationUnit();
		Set<URI> urisWithChanges = params.getChanges().stream().map(fileEvent -> URI.create(fileEvent.getUri()))
				.collect(Collectors.toSet());
		compile(urisWithChanges);
		visitAST();
	}

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams didChangeConfigurationParams) {
	}

	// --- REQUESTS

	@Override
	public CompletableFuture<Hover> hover(TextDocumentPositionParams params) {
		HoverProvider provider = new HoverProvider(astVisitor);
		return provider.provideHover(params.getTextDocument(), params.getPosition());
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
		return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
	}

	@Override
	public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams params) {
		DefinitionProvider provider = new DefinitionProvider(astVisitor);
		return provider.provideDefinition(params.getTextDocument(), params.getPosition());
	}

	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		ReferenceProvider provider = new ReferenceProvider(astVisitor);
		return provider.provideReferences(params.getTextDocument(), params.getPosition());
	}

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
			DocumentSymbolParams params) {
		DocumentSymbolProvider provider = new DocumentSymbolProvider(astVisitor);
		return provider.provideDocumentSymbols(params.getTextDocument());
	}

	@Override
	public CompletableFuture<List<? extends SymbolInformation>> symbol(WorkspaceSymbolParams params) {
		WorkspaceSymbolProvider provider = new WorkspaceSymbolProvider(astVisitor);
		return provider.provideWorkspaceSymbols(params.getQuery());
	}

	@Override
	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		RenameProvider provider = new RenameProvider(astVisitor, fileContentsTracker);
		return provider.provideRename(params);
	}

	// --- INTERNAL

	private void visitAST() {
		astVisitor = new ASTNodeVisitor();
		astVisitor.visitCompilationUnit(compilationUnit);
	}

	private void createOrUpdateCompilationUnit() {
		File tempDirectory = tempDirectoryPath.toFile();
		if (tempDirectory.exists()) {
			try {
				Files.walk(tempDirectoryPath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			} catch (IOException e) {
				System.err.println("Failed to delete target directory: " + tempDirectoryPath);
				return;
			}
		}
		if (!tempDirectory.mkdir()) {
			System.err.println("Failed to create target directory: " + tempDirectoryPath);
			return;
		}
		compilationUnit = new GroovyCompilationUnit(compilerConfig, new ErrorCollectorWithoutThrow(compilerConfig));
		addDirectoryToCompilationUnit(srcMainGroovyPath, true, true);
		addDirectoryToCompilationUnit(srcMainJavaPath, false, true);
		addDirectoryToCompilationUnit(srcTestGroovyPath, true, true);
		addDirectoryToCompilationUnit(srcTestJavaPath, false, true);
		fileContentsTracker.getOpenURIs().forEach(uri -> {
			Path path = Paths.get(uri);
			String contents = fileContentsTracker.getContents(uri);
			SourceUnit sourceUnit = new SourceUnit(path.toString(),
					new StringReaderSourceWithURI(contents, uri, compilationUnit.getConfiguration()),
					compilationUnit.getConfiguration(), compilationUnit.getClassLoader(),
					compilationUnit.getErrorCollector());
			compilationUnit.addSource(sourceUnit);
		});
	}

	private void addDirectoryToCompilationUnit(Path dirPath, boolean allowGroovy, boolean allowJava) {
		if (!dirPath.toFile().exists()) {
			return;
		}
		try {
			Files.walk(dirPath).forEach((filePath) -> {
				if (filePath.endsWith(FILE_EXTENSION_GROOVY)) {
					if (!allowGroovy) {
						return;
					}
				} else if (!filePath.endsWith(FILE_EXTENSION_JAVA)) {
					if (!allowJava) {
						return;
					}
				} else {
					return;
				}
				URI fileURI = filePath.toUri();
				// skip files that are open in memory
				if (!fileContentsTracker.isOpen(fileURI)) {
					File file = filePath.toFile();
					if (file.isFile()) {
						compilationUnit.addSource(file);
					}
				}
			});
		} catch (IOException e) {
			System.err.println("Failed to walk directory for source files: " + dirPath);
		}
	}

	private void compile(Set<URI> uris) {
		try {
			compilationUnit.compile();
		} catch (MultipleCompilationErrorsException e) {
			// ignore
		}
		Set<PublishDiagnosticsParams> diagnostics = handleErrorCollector(compilationUnit.getErrorCollector());
		diagnostics.stream().forEach(languageClient::publishDiagnostics);
	}

	private Set<PublishDiagnosticsParams> handleErrorCollector(ErrorCollector collector) {
		Map<URI, List<Diagnostic>> diagnosticsByFile = new HashMap<>();

		@SuppressWarnings("unchecked")
		List<Message> errors = collector.getErrors();
		if (errors != null) {
			errors.stream().filter((Object message) -> message instanceof SyntaxErrorMessage)
					.forEach((Object message) -> {
						SyntaxErrorMessage syntaxErrorMessage = (SyntaxErrorMessage) message;
						SyntaxException cause = syntaxErrorMessage.getCause();
						Range range = GroovyLanguageServerUtils.syntaxExceptionToRange(cause);
						Diagnostic diagnostic = new Diagnostic();
						diagnostic.setRange(range);
						diagnostic.setSeverity(cause.isFatal() ? DiagnosticSeverity.Error : DiagnosticSeverity.Warning);
						diagnostic.setMessage(cause.getMessage());
						URI uri = Paths.get(cause.getSourceLocator()).toUri();
						diagnosticsByFile.computeIfAbsent(uri, (key) -> new ArrayList<>()).add(diagnostic);
					});
		}

		Set<PublishDiagnosticsParams> result = diagnosticsByFile.entrySet().stream()
				.map(entry -> new PublishDiagnosticsParams(entry.getKey().toString(), entry.getValue()))
				.collect(Collectors.toSet());

		if (prevDiagnosticsByFile != null) {
			for (URI key : prevDiagnosticsByFile.keySet()) {
				if (!diagnosticsByFile.containsKey(key)) {
					// send an empty list of diagnostics for files that had
					// diagnostics previously or they won't be cleared
					result.add(new PublishDiagnosticsParams(key.toString(), new ArrayList<>()));
				}
			}
		}
		prevDiagnosticsByFile = diagnosticsByFile;
		return result;
	}
}