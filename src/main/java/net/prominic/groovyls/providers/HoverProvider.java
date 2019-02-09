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
package net.prominic.groovyls.providers;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.Variable;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import net.prominic.groovyls.compiler.ast.ASTNodeVisitor;
import net.prominic.groovyls.compiler.util.GroovyASTUtils;

public class HoverProvider {
	private static final String JAVA_OBJECT = "java.lang.Object";

	private ASTNodeVisitor ast;

	public HoverProvider(ASTNodeVisitor ast) {
		this.ast = ast;
	}

	public CompletableFuture<Hover> provideHover(TextDocumentIdentifier textDocument, Position position) {
		Hover hover = new Hover();
		List<Either<String, MarkedString>> contents = new ArrayList<>();
		hover.setContents(contents);

		URI uri = URI.create(textDocument.getUri());
		ASTNode offsetNode = ast.getNodeAtLineAndColumn(uri, position.getLine(), position.getCharacter());

		ASTNode definitionNode = GroovyASTUtils.getDefinition(offsetNode, ast);
		if (definitionNode == null) {
			return CompletableFuture.completedFuture(hover);
		}

		String content = getContent(definitionNode);
		if (content == null) {
			return CompletableFuture.completedFuture(hover);
		}

		contents.add(Either.forLeft(content));
		return CompletableFuture.completedFuture(hover);
	}

	private String getContent(ASTNode hoverNode) {
		if (hoverNode instanceof ClassNode) {
			ClassNode classNode = (ClassNode) hoverNode;
			StringBuilder builder = new StringBuilder();
			if (!classNode.isSyntheticPublic()) {
				builder.append("public ");
			}
			if (classNode.isAbstract()) {
				builder.append("abstract ");
			}
			if (classNode.isInterface()) {
				builder.append("interface ");
			} else if (classNode.isEnum()) {
				builder.append("enum ");
			} else {
				builder.append("class ");
			}
			builder.append(classNode.getName());

			ClassNode superClass = classNode.getSuperClass();
			if (superClass != null && !superClass.getName().equals(JAVA_OBJECT)) {
				builder.append("extends ");
				builder.append(superClass.getNameWithoutPackage());
			}
			return builder.toString();
		} else if (hoverNode instanceof MethodNode) {
			MethodNode methodNode = (MethodNode) hoverNode;
			StringBuilder builder = new StringBuilder();
			if (methodNode.isPublic()) {
				if (!methodNode.isSyntheticPublic()) {
					builder.append("public ");
				}
			} else if (methodNode.isProtected()) {
				builder.append("protected ");
			} else if (methodNode.isPrivate()) {
				builder.append("private ");
			}

			if (methodNode.isStatic()) {
				builder.append("static ");
			}

			if (methodNode.isFinal()) {
				builder.append("final ");
			}
			ClassNode returnType = methodNode.getReturnType();
			builder.append(returnType.getNameWithoutPackage());
			builder.append(" ");
			builder.append(methodNode.getName());
			builder.append("(");
			Parameter[] params = methodNode.getParameters();
			for (int i = 0; i < params.length; i++) {
				if (i > 0) {
					builder.append(", ");
				}
				Parameter paramNode = params[i];
				ClassNode paramType = paramNode.getType();
				builder.append(paramType.getNameWithoutPackage());
				builder.append(" ");
				builder.append(paramNode.getName());
			}
			builder.append(")");
			return builder.toString();
		} else if (hoverNode instanceof Variable) {
			Variable varNode = (Variable) hoverNode;
			StringBuilder builder = new StringBuilder();
			if (varNode instanceof FieldNode) {
				FieldNode fieldNode = (FieldNode) varNode;
				if (fieldNode.isPublic()) {
					builder.append("public ");
				}
				if (fieldNode.isProtected()) {
					builder.append("protected ");
				}
				if (fieldNode.isPrivate()) {
					builder.append("private ");
				}

				if (fieldNode.isFinal()) {
					builder.append("final ");
				}

				if (fieldNode.isStatic()) {
					builder.append("static ");
				}
			}
			ClassNode varType = varNode.getType();
			builder.append(varType.getNameWithoutPackage());
			builder.append(" ");
			builder.append(varNode.getName());
			return builder.toString();
		} else {
			System.err.println("*** hover not available for node: " + hoverNode);
		}
		return null;
	}
}