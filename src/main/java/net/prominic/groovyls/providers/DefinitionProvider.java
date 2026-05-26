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
package net.prominic.groovyls.providers;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import net.prominic.groovyls.compiler.ast.ASTNodeVisitor;
import net.prominic.groovyls.compiler.util.GroovyASTUtils;
import net.prominic.groovyls.util.GroovyLanguageServerUtils;

public class DefinitionProvider {
	private ASTNodeVisitor ast;

	public DefinitionProvider(ASTNodeVisitor ast) {
		this.ast = ast;
	}

	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> provideDefinition(
			TextDocumentIdentifier textDocument, Position position) {
		if (ast == null) {
			// this shouldn't happen, but let's avoid an exception if something
			// goes terribly wrong.
			return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
		}
		URI uri = URI.create(textDocument.getUri());
		ASTNode offsetNode = ast.getNodeAtLineAndColumn(uri, position.getLine(), position.getCharacter());
		if (offsetNode == null) {
			return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
		}

		ASTNode definitionNode = GroovyASTUtils.getDefinition(offsetNode, true, ast);
		if (definitionNode == null) {
			return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
		}

		URI definitionURI = ast.getURI(definitionNode);
		if (definitionURI == null) {
			definitionURI = uri;
		}

		Location location = GroovyLanguageServerUtils.astNodeToLocation(definitionNode, definitionURI);
		if (location == null) {
			if (definitionNode instanceof ConstructorNode) {
				// This will "fall-through" to the if-block below!
				definitionNode = ((ConstructorNode) definitionNode).getDeclaringClass();
				definitionURI = ast.getURI(definitionNode);
			}
			if (definitionNode instanceof ClassNode) {
				location = new Location(definitionURI.toString(), new Range(new Position(), new Position(1, 1)));
				return CompletableFuture.completedFuture(Either.forLeft(Collections.singletonList(location)));
			}
			return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
		}

		return CompletableFuture.completedFuture(Either.forLeft(Collections.singletonList(location)));
	}
}