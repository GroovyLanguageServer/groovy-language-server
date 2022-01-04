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
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.ParameterInformation;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.SignatureInformation;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.prominic.groovyls.config.CompilationUnitFactory;

class GroovyServicesSignatureHelpTests {
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
	void testSignatureHelpOnMethod() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class SignatureHelp {\n");
		contents.append("  public SignatureHelp() {\n");
		contents.append("    method(\n");
		contents.append("  }\n");
		contents.append("  public void method(int param0) {}\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(2, 11);
		SignatureHelp signatureHelp = services.signatureHelp(new SignatureHelpParams(textDocument, position)).get();
		List<SignatureInformation> signatures = signatureHelp.getSignatures();
		Assertions.assertEquals(1, signatures.size());
		SignatureInformation signature = signatures.get(0);
		Assertions.assertEquals("public void method(int param0)", signature.getLabel());
		List<ParameterInformation> params = signature.getParameters();
		Assertions.assertEquals(1, params.size());
		ParameterInformation param0 = params.get(0);
		Assertions.assertEquals("int param0", param0.getLabel().get());
		Assertions.assertEquals((int) 0, (int) signatureHelp.getActiveSignature());
		Assertions.assertEquals((int) 0, (int) signatureHelp.getActiveParameter());
	}

	@Test
	void testSignatureHelpOnMethodWithMultipleParameters() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class SignatureHelp {\n");
		contents.append("  public SignatureHelp() {\n");
		contents.append("    method(\n");
		contents.append("  }\n");
		contents.append("  public void method(int param0, String param1) {}\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(2, 11);
		SignatureHelp signatureHelp = services.signatureHelp(new SignatureHelpParams(textDocument, position)).get();
		List<SignatureInformation> signatures = signatureHelp.getSignatures();
		Assertions.assertEquals(1, signatures.size());
		SignatureInformation signature = signatures.get(0);
		Assertions.assertEquals("public void method(int param0, String param1)", signature.getLabel());
		List<ParameterInformation> params = signature.getParameters();
		Assertions.assertEquals(2, params.size());
		ParameterInformation param0 = params.get(0);
		Assertions.assertEquals("int param0", param0.getLabel().get());
		ParameterInformation param1 = params.get(1);
		Assertions.assertEquals("String param1", param1.getLabel().get());
		Assertions.assertEquals((int) 0, (int) signatureHelp.getActiveSignature());
		Assertions.assertEquals((int) 0, (int) signatureHelp.getActiveParameter());
	}

	@Test
	void testSignatureHelpOnMethodWithActiveParameter() throws Exception {
		Path filePath = srcRoot.resolve("Completion.groovy");
		String uri = filePath.toUri().toString();
		StringBuilder contents = new StringBuilder();
		contents.append("class SignatureHelp {\n");
		contents.append("  public SignatureHelp() {\n");
		contents.append("    method(123,\n");
		contents.append("  }\n");
		contents.append("  public void method(int param0, String param1) {}\n");
		contents.append("}");
		TextDocumentItem textDocumentItem = new TextDocumentItem(uri, LANGUAGE_GROOVY, 1, contents.toString());
		services.didOpen(new DidOpenTextDocumentParams(textDocumentItem));
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Position position = new Position(2, 15);
		SignatureHelp signatureHelp = services.signatureHelp(new SignatureHelpParams(textDocument, position)).get();
		List<SignatureInformation> signatures = signatureHelp.getSignatures();
		Assertions.assertEquals(1, signatures.size());
		SignatureInformation signature = signatures.get(0);
		Assertions.assertEquals("public void method(int param0, String param1)", signature.getLabel());
		List<ParameterInformation> params = signature.getParameters();
		Assertions.assertEquals(2, params.size());
		ParameterInformation param0 = params.get(0);
		Assertions.assertEquals("int param0", param0.getLabel().get());
		ParameterInformation param1 = params.get(1);
		Assertions.assertEquals("String param1", param1.getLabel().get());
		Assertions.assertEquals((int) 0, (int) signatureHelp.getActiveSignature());
		Assertions.assertEquals((int) 1, (int) signatureHelp.getActiveParameter());
	}
}