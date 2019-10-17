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

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelpOptions;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import net.prominic.groovyls.config.CompilationUnitFactory;
import net.prominic.groovyls.config.CustomizableUnitFactory;
import net.prominic.groovyls.config.ICompilationUnitFactory;

public class GroovyLanguageServer implements LanguageServer, LanguageClientAware {

    public static void main(String[] args) {
        GroovyLanguageServer server = new GroovyLanguageServer(new CustomizableUnitFactory());
        Launcher<LanguageClient> launcher = Launcher.createLauncher(server, LanguageClient.class, System.in,
                System.out);
        server.connect(launcher.getRemoteProxy());
        launcher.startListening();
    }

	private static final String ADDITIONAL_LIBS_FOLDER_KEY = "ADDITIONAL_CLASSPATH_FOLDER";

	private GroovyServices groovyServices;
	private ICompilationUnitFactory compilationUnitFactory;

    public GroovyLanguageServer() {
        this(new CompilationUnitFactory());
    }

    public GroovyLanguageServer(ICompilationUnitFactory compilationUnitFactory) {
		this.groovyServices = new GroovyServices(compilationUnitFactory);
		this.compilationUnitFactory = compilationUnitFactory;
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        String rootUriString = params.getRootUri();
        if (rootUriString != null) {
            URI uri = URI.create(params.getRootUri());
            Path workspaceRoot = Paths.get(uri);
            groovyServices.setWorkspaceRoot(workspaceRoot);
        }

		// get additional classpath folder config option
		JsonArray convertedObject = (JsonArray)params.getInitializationOptions();
		for (int i = 0; i < convertedObject.size(); i++) {
			JsonElement e = convertedObject.get(i);
			if (e instanceof JsonObject) {
				JsonObject jo = (JsonObject)e;
				if (jo.has(ADDITIONAL_LIBS_FOLDER_KEY)) {
					e = jo.get(ADDITIONAL_LIBS_FOLDER_KEY);
					if (e instanceof JsonPrimitive) {
						JsonPrimitive jp = (JsonPrimitive)e;
						if (compilationUnitFactory instanceof CustomizableUnitFactory) {
							((CustomizableUnitFactory)compilationUnitFactory).setAdditionalLibsFolders(jp.getAsString());
						}
					}
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
        groovyServices.connect(client);
    }
}
