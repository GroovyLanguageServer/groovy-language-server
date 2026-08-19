////////////////////////////////////////////////////////////////////////////////
// Copyright 2026 trustytrojan
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
// Author: trustytrojan
// No warranty of merchantability or fitness of any kind.
// Use this software at your own risk.
////////////////////////////////////////////////////////////////////////////////
package net.prominic.groovyls.providers;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.TextDocumentIdentifier;

import net.prominic.groovyls.util.FileContentsTracker;
import net.prominic.groovyls.compiler.ast.ASTNodeVisitor;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.ClassNode;
import net.prominic.groovyls.compiler.util.GroovyASTUtils;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import net.prominic.groovyls.util.GroovyLanguageServerUtils;

/**
 * Semantic tokens provider that emits tokens for functions/methods and local/global variables by analyzing the AST.
 *
 * This implementation uses the Groovy AST to precisely locate declarations and method calls,
 * ensuring that tokens are only emitted for actual code elements and never for string literals
 * or comments. Tokens are encoded using the LSP delta format.
 */
public class SemanticTokensProvider {
	private final FileContentsTracker fileContentsTracker;

	// token types used in the legend (must match order below)
	public static final List<String> TOKEN_TYPES = Collections.unmodifiableList(Arrays.asList(
			"namespace","class","enum","interface","struct","typeParameter","type",
			"parameter","variable","property","enumMember","event","function","method",
			"macro","keyword","modifier","comment","string","number","regexp","operator"
	));

	private final ASTNodeVisitor astVisitor;

	public SemanticTokensProvider(FileContentsTracker fileContentsTracker, ASTNodeVisitor astVisitor) {
		this.fileContentsTracker = fileContentsTracker;
		this.astVisitor = astVisitor;
	}

	public SemanticTokensLegend getLegend() {
		return new SemanticTokensLegend(TOKEN_TYPES, Collections.emptyList());
	}

	public SemanticTokens provideFull(TextDocumentIdentifier textDocument) {
		URI uri = URI.create(textDocument.getUri());
		String text = fileContentsTracker.getContents(uri);
		if (text == null || astVisitor == null || uri == null) {
			return new SemanticTokens(new ArrayList<>());
		}

		List<Token> tokens = new ArrayList<>();
		Set<String> emitted = new HashSet<>();
		List<ASTNode> nodes = astVisitor.getNodes(uri);

		for (ASTNode node : nodes) {
			// 0) Method calls: color method name when we can resolve a method on the receiver
			if (node instanceof MethodCallExpression) {
				processMethodCall((MethodCallExpression) node, tokens, emitted);
			}
			
			// 1) Handle property/field access expressions (e.g., this.x, Closure.DELEGATE_FIRST)
			if (node instanceof PropertyExpression) {
				processPropertyExpression((PropertyExpression) node, tokens, emitted);
			}

			// 2) Declarations: methods, variables, fields, properties, parameters
			processDeclaration(node, text, tokens, emitted);
		}

		if (tokens.isEmpty()) {
			return new SemanticTokens(new ArrayList<>());
		}

		tokens.sort(Comparator.comparingInt((Token t) -> t.line).thenComparingInt(t -> t.startChar));

		return encodeDeltaTokens(tokens);
	}

	private void processMethodCall(MethodCallExpression call, List<Token> tokens, Set<String> emitted) {
		String methodName = call.getMethodAsString();
		if (methodName == null || methodName.isEmpty()) return;

		List<MethodNode> overloads = GroovyASTUtils.getMethodOverloadsFromCallExpression(call, astVisitor);
		if (overloads == null || overloads.isEmpty()) return;

		Range methodRange = GroovyLanguageServerUtils.astNodeToRange(call.getMethod());
		if (methodRange == null) return;

		Position pos = new Position(methodRange.getStart().getLine(), methodRange.getStart().getCharacter());
		String key = pos.line + ":" + pos.col + ":" + methodName + ":" + "method";
		if (emitted.add(key)) {
			tokens.add(new Token(pos.line, pos.col, methodName.length(), tokenTypeIndex("method")));
		}
	}

	private void processPropertyExpression(PropertyExpression pe, List<Token> tokens, Set<String> emitted) {
		String propName = pe.getPropertyAsString();
		if (propName == null || propName.isEmpty()) return;

		ASTNode propNode = (ASTNode) pe.getProperty();
		Range propRange = GroovyLanguageServerUtils.astNodeToRange(propNode);
		if (propRange == null) return;

		boolean fieldExists = false;

		// Check the target object expression's ClassNode for a field/property of the same name
		if (!fieldExists && pe.getObjectExpression() != null) {
			ClassNode targetClass = GroovyASTUtils.getTypeOfNode(pe.getObjectExpression(), astVisitor);
			if (targetClass != null && (targetClass.getField(propName) != null || targetClass.getProperty(propName) != null)) {
				fieldExists = true;
			}
		}

		if (!fieldExists) return;

		Position pos = new Position(propRange.getStart().getLine(), propRange.getStart().getCharacter());
		String key = pos.line + ":" + pos.col + ":" + propName + ":" + "property";
		if (emitted.add(key)) {
			tokens.add(new Token(pos.line, pos.col, propName.length(), tokenTypeIndex("property")));
		}
	}

	// probably should be named `processSymbol` and/or should be split up by type a bit more
	private void processDeclaration(ASTNode node, String text, List<Token> tokens, Set<String> emitted) {
		Range range = GroovyLanguageServerUtils.astNodeToRange(node);
		if (range == null) return;

		if (node instanceof MethodNode && ((MethodNode) node).isConstructor()) {
			processConstructorDeclaration((MethodNode) node, text, range, tokens, emitted);
			return;
		}

		String name = getDeclarationName(node);
		if (name == null || "this".equals(name) || "super".equals(name)) return;

		int startOffset = lineColToOffset(text, range.getStart().getLine(), range.getStart().getCharacter());
		int endOffset = lineColToOffset(text, range.getEnd().getLine(), range.getEnd().getCharacter());
		if (startOffset < 0 || endOffset <= startOffset) return;

		int found = findExactTokenOffset(text, name, startOffset, endOffset);
		if (found == -1) return;

		Position pos = toLineCol(text, found);
		int tokenType = tokenTypeIndexFromNode(node);
		String key = pos.line + ":" + pos.col + ":" + name + ":" + tokenType;
		if (emitted.add(key)) {
			tokens.add(new Token(pos.line, pos.col, name.length(), tokenType));
		}
	}

	private int tokenTypeIndexFromNode(ASTNode node) {
		if (node instanceof MethodNode
				|| GroovyASTUtils.getTypeOfNode(node, astVisitor).equals(ClassHelper.CLOSURE_TYPE))
			return tokenTypeIndex("function");
		if (node instanceof ClassNode || node instanceof ImportNode)
			return tokenTypeIndex("class");
		return tokenTypeIndex("variable");
	}

	private void processConstructorDeclaration(MethodNode mn, String text, Range range, List<Token> tokens, Set<String> emitted) {
		ClassNode declaringClass = mn.getDeclaringClass();
		String className = declaringClass != null ? declaringClass.getNameWithoutPackage() : null;
		if (className == null || className.isEmpty()) return;

		int startOffsetCtor = lineColToOffset(text, range.getStart().getLine(), range.getStart().getCharacter());
		int endOffsetCtor = lineColToOffset(text, range.getEnd().getLine(), range.getEnd().getCharacter());
		
		int foundCtor = findExactTokenOffset(text, className, startOffsetCtor, endOffsetCtor);
		if (foundCtor == -1) return;

		Position posCtor = toLineCol(text, foundCtor);
		int tokenTypeCtor = tokenTypeIndex("class");
		String keyCtor = posCtor.line + ":" + posCtor.col + ":" + className + ":" + tokenTypeCtor;
		if (emitted.add(keyCtor)) {
			tokens.add(new Token(posCtor.line, posCtor.col, className.length(), tokenTypeCtor));
		}
	}

	private String getDeclarationName(ASTNode node) {
		if (node instanceof MethodNode) return ((MethodNode) node).getName();
		if (node instanceof Variable) return ((Variable) node).getName();
		if (node instanceof FieldNode) return ((FieldNode) node).getName();
		if (node instanceof PropertyNode) return ((PropertyNode) node).getName();
		if (node instanceof Parameter) return ((Parameter) node).getName();
		if (node instanceof ClassNode) return ((ClassNode) node).getName();
		if (node instanceof ImportNode) return ((ImportNode) node).getClassName();
		return null;
	}

	private int findExactTokenOffset(String text, String name, int startOffset, int endOffset) {
		int found = startOffset;
		while (found >= 0) {
			found = text.indexOf(name, found);
			if (found == -1 || found >= endOffset) {
				return -1;
			}
			boolean beforeValid = (found == 0) || !Character.isJavaIdentifierPart(text.charAt(found - 1));
			boolean afterValid = (found + name.length() >= text.length()) || !Character.isJavaIdentifierPart(text.charAt(found + name.length()));
			if (beforeValid && afterValid) {
				return found;
			}
			found++;
		}
		return -1;
	}

	private SemanticTokens encodeDeltaTokens(List<Token> tokens) {
		List<Integer> data = new ArrayList<>();
		int prevLine = 0;
		int prevChar = 0;
		boolean first = true;
		for (Token t : tokens) {
			int deltaLine = first ? t.line : t.line - prevLine;
			int deltaStart = first ? t.startChar : (deltaLine == 0 ? t.startChar - prevChar : t.startChar);
			data.add(deltaLine);
			data.add(deltaStart);
			data.add(t.length);
			data.add(t.tokenType);
			data.add(0); // modifiers bitset

			prevLine = t.line;
			prevChar = t.startChar;
			first = false;
		}
		return new SemanticTokens(data);
	}

	private int lineColToOffset(String text, int line, int col) {
		if (line < 0) return -1;
		int curLine = 0;
		int offset = 0;
		int len = text.length();
		while (offset < len && curLine < line) {
			if (text.charAt(offset) == '\n') {
				curLine++;
			}
			offset++;
		}
		if (curLine != line) return -1;
		return Math.min(offset + col, len);
	}

	private int tokenTypeIndex(String type) {
		int idx = TOKEN_TYPES.indexOf(type);
		return idx >= 0 ? idx : 0;
	}

	private static class Token {
		final int line;
		final int startChar;
		final int length;
		final int tokenType;

		Token(int line, int startChar, int length, int tokenType) {
			this.line = line;
			this.startChar = startChar;
			this.length = length;
			this.tokenType = tokenType;
		}
	}

	private static class Position {
		final int line;
		final int col;

		Position(int line, int col) {
			this.line = line;
			this.col = col;
		}
	}

	private Position toLineCol(String text, int offset) {
		int line = 0;
		int col = 0;
		int i = 0;
		while (i < offset) {
			char c = text.charAt(i);
			if (c == '\n') {
				line++;
				col = 0;
			} else {
				col++;
			}
			i++;
		}
		return new Position(line, col);
	}
}