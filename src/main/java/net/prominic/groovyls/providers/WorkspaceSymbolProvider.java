////////////////////////////////////////////////////////////////////////////////
// Copyright 2021 Prominic.NET, Inc.
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
import java.util.stream.Collectors;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.eclipse.lsp4j.SymbolInformation;

import net.prominic.groovyls.compiler.ast.ASTNodeVisitor;
import net.prominic.groovyls.compiler.util.GroovyASTUtils;
import net.prominic.groovyls.util.GroovyLanguageServerUtils;

public class WorkspaceSymbolProvider {
	private ASTNodeVisitor ast;

	public WorkspaceSymbolProvider(ASTNodeVisitor ast) {
		this.ast = ast;
	}

	public CompletableFuture<List<? extends SymbolInformation>> provideWorkspaceSymbols(String query) {
		if (ast == null) {
			// this shouldn't happen, but let's avoid an exception if something
			// goes terribly wrong.
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
		String lowerCaseQuery = query.toLowerCase();
		List<ASTNode> nodes = ast.getNodes();
		List<SymbolInformation> symbols = nodes.stream().filter(node -> {
			String name = null;
			if (node instanceof ClassNode) {
				ClassNode classNode = (ClassNode) node;
				name = classNode.getName();
			} else if (node instanceof MethodNode) {
				MethodNode methodNode = (MethodNode) node;
				name = methodNode.getName();
			} else if (node instanceof FieldNode) {
				FieldNode fieldNode = (FieldNode) node;
				name = fieldNode.getName();
			} else if (node instanceof PropertyNode) {
				PropertyNode propNode = (PropertyNode) node;
				name = propNode.getName();
			}
			if (name == null) {
				return false;
			}
			return name.toLowerCase().contains(lowerCaseQuery);
		}).map(node -> {
			URI uri = ast.getURI(node);
			if (node instanceof ClassNode) {
				ClassNode classNode = (ClassNode) node;
				return GroovyLanguageServerUtils.astNodeToSymbolInformation(classNode, uri, null);
			}
			ClassNode classNode = (ClassNode) GroovyASTUtils.getEnclosingNodeOfType(node, ClassNode.class, ast);
			if (node instanceof MethodNode) {
				MethodNode methodNode = (MethodNode) node;
				return GroovyLanguageServerUtils.astNodeToSymbolInformation(methodNode, uri, classNode.getName());
			}
			if (node instanceof PropertyNode) {
				PropertyNode propNode = (PropertyNode) node;
				return GroovyLanguageServerUtils.astNodeToSymbolInformation(propNode, uri, classNode.getName());
			}
			if (node instanceof FieldNode) {
				FieldNode fieldNode = (FieldNode) node;
				return GroovyLanguageServerUtils.astNodeToSymbolInformation(fieldNode, uri, classNode.getName());
			}
			// this should never happen
			return null;
		}).filter(symbolInformation -> symbolInformation != null).collect(Collectors.toList());
		return CompletableFuture.completedFuture(symbols);
	}
}