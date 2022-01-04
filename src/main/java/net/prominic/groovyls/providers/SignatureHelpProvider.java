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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCall;
import org.eclipse.lsp4j.ParameterInformation;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;
import org.eclipse.lsp4j.TextDocumentIdentifier;

import net.prominic.groovyls.compiler.ast.ASTNodeVisitor;
import net.prominic.groovyls.compiler.util.GroovyASTUtils;
import net.prominic.groovyls.util.GroovyLanguageServerUtils;
import net.prominic.groovyls.util.GroovyNodeToStringUtils;

public class SignatureHelpProvider {
	private ASTNodeVisitor ast;

	public SignatureHelpProvider(ASTNodeVisitor ast) {
		this.ast = ast;
	}

	public CompletableFuture<SignatureHelp> provideSignatureHelp(TextDocumentIdentifier textDocument,
			Position position) {
		if (ast == null) {
			// this shouldn't happen, but let's avoid an exception if something
			// goes terribly wrong.
			return CompletableFuture.completedFuture(new SignatureHelp(Collections.emptyList(), -1, -1));
		}
		URI uri = URI.create(textDocument.getUri());
		ASTNode offsetNode = ast.getNodeAtLineAndColumn(uri, position.getLine(), position.getCharacter());
		if (offsetNode == null) {
			return CompletableFuture.completedFuture(new SignatureHelp(Collections.emptyList(), -1, -1));
		}
		int activeParamIndex = -1;
		MethodCall methodCall = null;
		ASTNode parentNode = ast.getParent(offsetNode);

		if (offsetNode instanceof ArgumentListExpression) {
			methodCall = (MethodCall) parentNode;

			ArgumentListExpression argsList = (ArgumentListExpression) offsetNode;
			List<Expression> expressions = argsList.getExpressions();
			activeParamIndex = getActiveParameter(position, expressions);
		}

		if (methodCall == null) {
			return CompletableFuture.completedFuture(new SignatureHelp(Collections.emptyList(), -1, -1));
		}

		List<MethodNode> methods = GroovyASTUtils.getMethodOverloadsFromCallExpression(methodCall, ast);
		if (methods.isEmpty()) {
			return CompletableFuture.completedFuture(new SignatureHelp(Collections.emptyList(), -1, -1));
		}

		List<SignatureInformation> sigInfos = new ArrayList<>();
		for (MethodNode method : methods) {
			List<ParameterInformation> parameters = new ArrayList<>();
			Parameter[] methodParams = method.getParameters();
			for (int i = 0; i < methodParams.length; i++) {
				Parameter methodParam = methodParams[i];

				ParameterInformation paramInfo = new ParameterInformation();
				paramInfo.setLabel(GroovyNodeToStringUtils.variableToString(methodParam, ast));
				parameters.add(paramInfo);
			}
			SignatureInformation sigInfo = new SignatureInformation();
			sigInfo.setLabel(GroovyNodeToStringUtils.methodToString(method, ast));
			sigInfo.setParameters(parameters);
			sigInfos.add(sigInfo);
		}

		MethodNode bestMethod = GroovyASTUtils.getMethodFromCallExpression(methodCall, ast, activeParamIndex);
		int activeSignature = methods.indexOf(bestMethod);

		return CompletableFuture.completedFuture(new SignatureHelp(sigInfos, activeSignature, activeParamIndex));
	}

	private int getActiveParameter(Position position, List<Expression> expressions) {
		for (int i = 0; i < expressions.size(); i++) {
			Expression expr = expressions.get(i);
			Range exprRange = GroovyLanguageServerUtils.astNodeToRange(expr);
			if (exprRange == null) {
				continue;
			}
			if (position.getLine() < exprRange.getEnd().getLine()) {
				return i;
			}
			if (position.getLine() == exprRange.getEnd().getLine()
					&& position.getCharacter() <= exprRange.getEnd().getCharacter()) {
				return i;
			}
		}
		return expressions.size();
	}
}