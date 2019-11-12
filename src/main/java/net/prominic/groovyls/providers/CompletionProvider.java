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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.eclipse.lsp4j.CompletionContext;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import net.prominic.groovyls.compiler.ast.ASTNodeVisitor;
import net.prominic.groovyls.compiler.util.GroovyASTUtils;
import net.prominic.groovyls.util.GroovyLanguageServerUtils;

public class CompletionProvider {
	private ASTNodeVisitor ast;

	public CompletionProvider(ASTNodeVisitor ast) {
		this.ast = ast;
	}

	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> provideCompletion(
			TextDocumentIdentifier textDocument, Position position, CompletionContext context) {
		if (ast == null) {
			//this shouldn't happen, but let's avoid an exception if something
			//goes terribly wrong.
			return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
		}
		URI uri = URI.create(textDocument.getUri());
		ASTNode offsetNode = ast.getNodeAtLineAndColumn(uri, position.getLine(), position.getCharacter());
		if (offsetNode == null) {
			return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
		}
		ASTNode parentNode = ast.getParent(offsetNode);

		List<CompletionItem> items = new ArrayList<>();

		if (offsetNode instanceof PropertyExpression) {
			populateItemsFromPropertyExpression((PropertyExpression) offsetNode, position, items);
		} else if (parentNode instanceof PropertyExpression) {
			populateItemsFromPropertyExpression((PropertyExpression) parentNode, position, items);
		} else if (offsetNode instanceof MethodCallExpression) {
			populateItemsFromMethodCallExpression((MethodCallExpression) offsetNode, position, items);
		} else if (parentNode instanceof MethodCallExpression) {
			populateItemsFromMethodCallExpression((MethodCallExpression) parentNode, position, items);
		} else if (offsetNode instanceof VariableExpression) {
			populateItemsFromVariableExpression((VariableExpression) offsetNode, position, items);
		} else if (offsetNode instanceof MethodNode) {
			populateItemsFromMethodNode((MethodNode) offsetNode, position, items);
		} else if (offsetNode instanceof Statement) {
			populateItemsFromStatement((Statement) offsetNode, position, items);
		}

		return CompletableFuture.completedFuture(Either.forLeft(items));
	}

	private void populateItemsFromPropertyExpression(PropertyExpression propExpr, Position position,
			List<CompletionItem> items) {
		Range propertyRange = GroovyLanguageServerUtils.astNodeToRange(propExpr.getProperty());
		String memberName = getMemberName(propExpr.getPropertyAsString(), propertyRange, position);
		populateItemsFromExpression(propExpr.getObjectExpression(), memberName, items);
	}

	private void populateItemsFromMethodCallExpression(MethodCallExpression methodCallExpr, Position position,
			List<CompletionItem> items) {
		Range methodRange = GroovyLanguageServerUtils.astNodeToRange(methodCallExpr.getMethod());
		String memberName = getMemberName(methodCallExpr.getMethodAsString(), methodRange, position);
		populateItemsFromExpression(methodCallExpr.getObjectExpression(), memberName, items);
	}

	private void populateItemsFromVariableExpression(VariableExpression varExpr, Position position,
			List<CompletionItem> items) {
		Range varRange = GroovyLanguageServerUtils.astNodeToRange(varExpr);
		String memberName = getMemberName(varExpr.getName(), varRange, position);
		ClassNode enclosingClass = GroovyASTUtils.getEnclosingClass(varExpr, ast);
		populateItemsFromPropertiesAndFields(enclosingClass.getProperties(), enclosingClass.getFields(), memberName,
				items);
		populateItemsFromMethods(enclosingClass.getMethods(), memberName, items);
	}

	private void populateItemsFromPropertiesAndFields(List<PropertyNode> properties, List<FieldNode> fields,
			String memberNamePrefix, List<CompletionItem> items) {
		Set<String> foundNames = new HashSet<>();
		List<CompletionItem> propItems = properties.stream().filter(property -> {
			String name = property.getName();
			//sometimes, a property and a field will have the same name
			if (name.startsWith(memberNamePrefix) && !foundNames.contains(name)) {
				foundNames.add(name);
				return true;
			}
			return false;
		}).map(property -> {
			CompletionItem item = new CompletionItem();
			item.setLabel(property.getName());
			item.setKind(GroovyLanguageServerUtils.astNodeToCompletionItemKind(property));
			return item;
		}).collect(Collectors.toList());
		items.addAll(propItems);
		List<CompletionItem> fieldItems = fields.stream().filter(field -> {
			String name = field.getName();
			//sometimes, a property and a field will have the same name
			if (name.startsWith(memberNamePrefix) && !foundNames.contains(name)) {
				foundNames.add(name);
				return true;
			}
			return false;
		}).map(field -> {
			CompletionItem item = new CompletionItem();
			item.setLabel(field.getName());
			item.setKind(GroovyLanguageServerUtils.astNodeToCompletionItemKind(field));
			return item;
		}).collect(Collectors.toList());
		items.addAll(fieldItems);
	}

	private void populateItemsFromMethods(List<MethodNode> methods, String memberNamePrefix,
			List<CompletionItem> items) {
		Set<String> foundMethods = new HashSet<>();
		List<CompletionItem> methodItems = methods.stream().filter(method -> {
			String methodName = method.getName();
			//overloads can cause duplicates
			if (methodName.startsWith(memberNamePrefix) && !foundMethods.contains(methodName)) {
				foundMethods.add(methodName);
				return true;
			}
			return false;
		}).map(method -> {
			CompletionItem item = new CompletionItem();
			item.setLabel(method.getName());
			item.setKind(GroovyLanguageServerUtils.astNodeToCompletionItemKind(method));
			return item;
		}).collect(Collectors.toList());
		items.addAll(methodItems);
	}

	private void populateItemsFromExpression(Expression leftSide, String memberNamePrefix, List<CompletionItem> items) {
		List<PropertyNode> properties = GroovyASTUtils.getPropertiesForLeftSideOfPropertyExpression(leftSide, ast);
		List<FieldNode> fields = GroovyASTUtils.getFieldsForLeftSideOfPropertyExpression(leftSide, ast);
		populateItemsFromPropertiesAndFields(properties, fields, memberNamePrefix, items);

		List<MethodNode> methods = GroovyASTUtils.getMethodsForLeftSideOfPropertyExpression(leftSide, ast);
		populateItemsFromMethods(methods, memberNamePrefix, items);
	}

	private void populateItemsFromVariableScope(VariableScope variableScope, Position position,
			List<CompletionItem> items) {
		List<CompletionItem> variableItems = variableScope.getDeclaredVariables().values().stream().map(variable -> {
			CompletionItem item = new CompletionItem();
			item.setLabel(variable.getName());
			item.setKind(GroovyLanguageServerUtils.astNodeToCompletionItemKind((ASTNode) variable));
			return item;
		}).collect(Collectors.toList());
		items.addAll(variableItems);
	}

	private void populateItemsFromBlockStatement(BlockStatement block, Position position, List<CompletionItem> items) {
		populateItemsFromVariableScope(block.getVariableScope(), position, items);
	}

	private void populateItemsFromMethodNode(MethodNode method, Position position, List<CompletionItem> items) {
		populateItemsFromVariableScope(method.getVariableScope(), position, items);
	}

	private void populateItemsFromStatement(Statement statement, Position position, List<CompletionItem> items) {

		ASTNode current = statement;
		while (current != null) {
			if (current instanceof MethodNode) {
				populateItemsFromMethodNode((MethodNode) current, position, items);
				break;
			}
			if (current instanceof BlockStatement) {
				populateItemsFromBlockStatement((BlockStatement) current, position, items);
			}
			current = ast.getParent(current);
		}
	}

	private String getMemberName(String memberName, Range range, Position position) {
		if (position.getLine() == range.getStart().getLine()
				&& position.getCharacter() > range.getStart().getCharacter()) {
			int length = position.getCharacter() - range.getStart().getCharacter();
			if (length > 0 && length <= memberName.length()) {
				return memberName.substring(0, length);
			}
		}
		return "";
	}
}