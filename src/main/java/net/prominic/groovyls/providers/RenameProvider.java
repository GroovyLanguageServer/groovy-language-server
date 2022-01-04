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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.RenameFile;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import net.prominic.groovyls.compiler.ast.ASTNodeVisitor;
import net.prominic.groovyls.compiler.util.GroovyASTUtils;
import net.prominic.groovyls.util.FileContentsTracker;
import net.prominic.groovyls.util.GroovyLanguageServerUtils;
import net.prominic.lsp.utils.Ranges;

public class RenameProvider {
	private ASTNodeVisitor ast;
	private FileContentsTracker files;

	public RenameProvider(ASTNodeVisitor ast, FileContentsTracker files) {
		this.ast = ast;
		this.files = files;
	}

	public CompletableFuture<WorkspaceEdit> provideRename(RenameParams renameParams) {
		TextDocumentIdentifier textDocument = renameParams.getTextDocument();
		Position position = renameParams.getPosition();
		String newName = renameParams.getNewName();

		Map<String, List<TextEdit>> textEditChanges = new HashMap<>();
		List<Either<TextDocumentEdit, ResourceOperation>> documentChanges = new ArrayList<>();
		WorkspaceEdit workspaceEdit = new WorkspaceEdit(documentChanges);

		if (ast == null) {
			// this shouldn't happen, but let's avoid an exception if something
			// goes terribly wrong.
			return CompletableFuture.completedFuture(workspaceEdit);
		}
		URI documentURI = URI.create(textDocument.getUri());
		ASTNode offsetNode = ast.getNodeAtLineAndColumn(documentURI, position.getLine(), position.getCharacter());
		if (offsetNode == null) {
			return CompletableFuture.completedFuture(workspaceEdit);
		}

		List<ASTNode> references = GroovyASTUtils.getReferences(offsetNode, ast);
		references.forEach(node -> {
			URI uri = ast.getURI(node);
			if (uri == null) {
				uri = documentURI;
			}

			String contents = getPartialNodeText(uri, node);
			if (contents == null) {
				// can't find the text? skip it
				return;
			}
			Range range = GroovyLanguageServerUtils.astNodeToRange(node);
			if (range == null) {
				// can't find the range? skip it
				return;
			}
			Position start = range.getStart();
			Position end = range.getEnd();
			end.setLine(start.getLine());
			end.setCharacter(start.getCharacter() + contents.length());

			TextEdit textEdit = null;
			if (node instanceof ClassNode) {
				ClassNode classNode = (ClassNode) node;
				textEdit = createTextEditToRenameClassNode(classNode, newName, contents, range);
				if (textEdit != null && ast.getParent(classNode) == null) {
					String newURI = uri.toString();
					int slashIndex = newURI.lastIndexOf("/");
					int dotIndex = newURI.lastIndexOf(".");
					newURI = newURI.substring(0, slashIndex + 1) + newName + newURI.substring(dotIndex);

					RenameFile renameFile = new RenameFile();
					renameFile.setOldUri(uri.toString());
					renameFile.setNewUri(newURI);
					documentChanges.add(Either.forRight(renameFile));
				}
			} else if (node instanceof MethodNode) {
				MethodNode methodNode = (MethodNode) node;
				textEdit = createTextEditToRenameMethodNode(methodNode, newName, contents, range);
			} else if (node instanceof PropertyNode) {
				PropertyNode propNode = (PropertyNode) node;
				textEdit = createTextEditToRenamePropertyNode(propNode, newName, contents, range);
			} else if (node instanceof ConstantExpression || node instanceof VariableExpression) {
				textEdit = new TextEdit();
				textEdit.setNewText(newName);
				textEdit.setRange(range);
			}
			if (textEdit == null) {
				return;
			}

			if (!textEditChanges.containsKey(uri.toString())) {
				textEditChanges.put(uri.toString(), new ArrayList<>());
			}
			List<TextEdit> textEdits = textEditChanges.get(uri.toString());
			textEdits.add(textEdit);
		});

		for (String uri : textEditChanges.keySet()) {
			List<TextEdit> textEdits = textEditChanges.get(uri);

			VersionedTextDocumentIdentifier versionedIdentifier = new VersionedTextDocumentIdentifier(uri, null);
			TextDocumentEdit textDocumentEdit = new TextDocumentEdit(versionedIdentifier, textEdits);
			documentChanges.add(0, Either.forLeft(textDocumentEdit));
		}

		return CompletableFuture.completedFuture(workspaceEdit);
	}

	private String getPartialNodeText(URI uri, ASTNode node) {
		Range range = GroovyLanguageServerUtils.astNodeToRange(node);
		if (range == null) {
			return null;
		}
		String contents = files.getContents(uri);
		if (contents == null) {
			return null;
		}
		return Ranges.getSubstring(contents, range, 1);
	}

	private TextEdit createTextEditToRenameClassNode(ClassNode classNode, String newName, String text, Range range) {
		// the AST doesn't give us access to the name location, so we
		// need to find it manually
		String className = classNode.getNameWithoutPackage();
		int dollarIndex = className.indexOf('$');
		if (dollarIndex != 01) {
			// it's an inner class, so remove the outer name prefix
			className = className.substring(dollarIndex + 1);
		}

		Pattern classPattern = Pattern.compile("(class\\s+)" + className + "\\b");
		Matcher classMatcher = classPattern.matcher(text);
		if (!classMatcher.find()) {
			// couldn't find the name!
			return null;
		}
		String prefix = classMatcher.group(1);

		Position start = range.getStart();
		Position end = range.getEnd();
		end.setCharacter(start.getCharacter() + classMatcher.end());
		start.setCharacter(start.getCharacter() + prefix.length() + classMatcher.start());

		TextEdit textEdit = new TextEdit();
		textEdit.setRange(range);
		textEdit.setNewText(newName);
		return textEdit;
	}

	private TextEdit createTextEditToRenameMethodNode(MethodNode methodNode, String newName, String text, Range range) {
		// the AST doesn't give us access to the name location, so we
		// need to find it manually
		Pattern methodPattern = Pattern.compile("\\b" + methodNode.getName() + "\\b(?=\\s*\\()");
		Matcher methodMatcher = methodPattern.matcher(text);
		if (!methodMatcher.find()) {
			// couldn't find the name!
			return null;
		}

		Position start = range.getStart();
		Position end = range.getEnd();
		end.setCharacter(start.getCharacter() + methodMatcher.end());
		start.setCharacter(start.getCharacter() + methodMatcher.start());

		TextEdit textEdit = new TextEdit();
		textEdit.setRange(range);
		textEdit.setNewText(newName);
		return textEdit;
	}

	private TextEdit createTextEditToRenamePropertyNode(PropertyNode propNode, String newName, String text,
			Range range) {
		// the AST doesn't give us access to the name location, so we
		// need to find it manually
		Pattern propPattern = Pattern.compile("\\b" + propNode.getName() + "\\b");
		Matcher propMatcher = propPattern.matcher(text);
		if (!propMatcher.find()) {
			// couldn't find the name!
			return null;
		}

		Position start = range.getStart();
		Position end = range.getEnd();
		end.setCharacter(start.getCharacter() + propMatcher.end());
		start.setCharacter(start.getCharacter() + propMatcher.start());

		TextEdit textEdit = new TextEdit();
		textEdit.setRange(range);
		textEdit.setNewText(newName);
		return textEdit;
	}
}