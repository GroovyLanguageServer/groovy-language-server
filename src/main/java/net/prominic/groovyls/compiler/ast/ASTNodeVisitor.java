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
package net.prominic.groovyls.compiler.ast;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ArrayExpression;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BitwiseNegationExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ClosureListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.codehaus.groovy.ast.expr.GStringExpression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.MethodPointerExpression;
import org.codehaus.groovy.ast.expr.NotExpression;
import org.codehaus.groovy.ast.expr.PostfixExpression;
import org.codehaus.groovy.ast.expr.PrefixExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.RangeExpression;
import org.codehaus.groovy.ast.expr.SpreadExpression;
import org.codehaus.groovy.ast.expr.SpreadMapExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.UnaryMinusExpression;
import org.codehaus.groovy.ast.expr.UnaryPlusExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.AssertStatement;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.BreakStatement;
import org.codehaus.groovy.ast.stmt.CaseStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.ContinueStatement;
import org.codehaus.groovy.ast.stmt.DoWhileStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.SwitchStatement;
import org.codehaus.groovy.ast.stmt.SynchronizedStatement;
import org.codehaus.groovy.ast.stmt.ThrowStatement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.ast.stmt.WhileStatement;
import org.codehaus.groovy.classgen.BytecodeExpression;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import net.prominic.groovyls.util.GroovyLanguageServerUtils;
import net.prominic.lsp.utils.Positions;
import net.prominic.lsp.utils.Ranges;

public class ASTNodeVisitor extends ClassCodeVisitorSupport {
	private class ASTLookupKey {
		public ASTLookupKey(ASTNode node) {
			this.node = node;
		}

		private ASTNode node;

		@Override
		public boolean equals(Object o) {
			// some ASTNode subclasses, like ClassNode, override equals() with
			// comparisons that are not strict. we need strict.
			ASTLookupKey other = (ASTLookupKey) o;
			return node == other.node;
		}

		@Override
		public int hashCode() {
			return node.hashCode();
		}
	}

	private class ASTNodeLookupData {
		public ASTNode parent;
		public URI uri;
	}

	private SourceUnit sourceUnit;

	@Override
	protected SourceUnit getSourceUnit() {
		return sourceUnit;
	}

	private Stack<ASTNode> stack = new Stack<>();
	private Map<URI, List<ASTNode>> nodesByURI = new HashMap<>();
	private Map<URI, List<ClassNode>> classNodesByURI = new HashMap<>();
	private Map<ASTLookupKey, ASTNodeLookupData> lookup = new HashMap<>();

	private void pushASTNode(ASTNode node) {
		boolean isSynthetic = false;
		if (node instanceof AnnotatedNode) {
			AnnotatedNode annotatedNode = (AnnotatedNode) node;
			isSynthetic = annotatedNode.isSynthetic();
		}
		if (!isSynthetic) {
			URI uri = sourceUnit.getSource().getURI();
			nodesByURI.get(uri).add(node);

			ASTNodeLookupData data = new ASTNodeLookupData();
			data.uri = uri;
			if (stack.size() > 0) {
				data.parent = stack.lastElement();
			}
			lookup.put(new ASTLookupKey(node), data);
		}

		stack.add(node);
	}

	private void popASTNode() {
		stack.pop();
	}

	public List<ClassNode> getClassNodes() {
		List<ClassNode> result = new ArrayList<>();
		for (List<ClassNode> nodes : classNodesByURI.values()) {
			result.addAll(nodes);
		}
		return result;
	}

	public List<ASTNode> getNodes() {
		List<ASTNode> result = new ArrayList<>();
		for (List<ASTNode> nodes : nodesByURI.values()) {
			result.addAll(nodes);
		}
		return result;
	}

	public List<ASTNode> getNodes(URI uri) {
		List<ASTNode> nodes = nodesByURI.get(uri);
		if (nodes == null) {
			return Collections.emptyList();
		}
		return nodes;
	}

	public ASTNode getNodeAtLineAndColumn(URI uri, int line, int column) {
		Position position = new Position(line, column);
		Map<ASTNode, Range> nodeToRange = new HashMap<>();
		List<ASTNode> nodes = nodesByURI.get(uri);
		if (nodes == null) {
			return null;
		}
		List<ASTNode> foundNodes = nodes.stream().filter(node -> {
			if (node.getLineNumber() == -1) {
				// can't be the offset node if it has no position
				// also, do this first because it's the fastest comparison
				return false;
			}
			Range range = GroovyLanguageServerUtils.astNodeToRange(node);
			if (range == null) {
				return false;
			}
			boolean result = Ranges.contains(range, position);
			if (result) {
				// save the range object to avoid creating it again when we
				// sort the nodes
				nodeToRange.put(node, range);
			}
			return result;
		}).sorted((n1, n2) -> {
			int result = Positions.COMPARATOR.reversed().compare(nodeToRange.get(n1).getStart(),
					nodeToRange.get(n2).getStart());
			if (result != 0) {
				return result;
			}
			result = Positions.COMPARATOR.compare(nodeToRange.get(n1).getEnd(), nodeToRange.get(n2).getEnd());
			if (result != 0) {
				return result;
			}
			// n1 and n2 have the same range
			if (contains(n1, n2)) {
				if (n1 instanceof ClassNode && n2 instanceof ConstructorNode) {
					return -1;
				}
				return 1;
			} else if (contains(n2, n1)) {
				if (n2 instanceof ClassNode && n1 instanceof ConstructorNode) {
					return 1;
				}
				return -1;
			}
			return 0;
		}).collect(Collectors.toList());
		if (foundNodes.size() == 0) {
			return null;
		}
		return foundNodes.get(0);
	}

	public ASTNode getParent(ASTNode child) {
		if (child == null) {
			return null;
		}
		ASTNodeLookupData data = lookup.get(new ASTLookupKey(child));
		if (data == null) {
			return null;
		}
		return data.parent;
	}

	public boolean contains(ASTNode ancestor, ASTNode descendant) {
		ASTNode current = getParent(descendant);
		while (current != null) {
			if (current.equals(ancestor)) {
				return true;
			}
			current = getParent(current);
		}
		return false;
	}

	public URI getURI(ASTNode node) {
		ASTNodeLookupData data = lookup.get(new ASTLookupKey(node));
		if (data == null) {
			return null;
		}
		return data.uri;
	}

	public void visitCompilationUnit(CompilationUnit unit) {
		nodesByURI.clear();
		classNodesByURI.clear();
		lookup.clear();
		unit.iterator().forEachRemaining(sourceUnit -> {
			visitSourceUnit(sourceUnit);
		});
	}

	public void visitCompilationUnit(CompilationUnit unit, Collection<URI> uris) {
		uris.forEach(uri -> {
			// clear all old nodes so that they may be replaced
			List<ASTNode> nodes = nodesByURI.remove(uri);
			if (nodes != null) {
				nodes.forEach(node -> {
					lookup.remove(new ASTLookupKey(node));
				});
			}
			classNodesByURI.remove(uri);
		});
		unit.iterator().forEachRemaining(sourceUnit -> {
			URI uri = sourceUnit.getSource().getURI();
			if (!uris.contains(uri)) {
				return;
			}
			visitSourceUnit(sourceUnit);
		});
	}

	public void visitSourceUnit(SourceUnit unit) {
		sourceUnit = unit;
		URI uri = sourceUnit.getSource().getURI();
		nodesByURI.put(uri, new ArrayList<>());
		classNodesByURI.put(uri, new ArrayList<>());
		stack.clear();
		ModuleNode moduleNode = unit.getAST();
		if (moduleNode != null) {
			visitModule(moduleNode);
		}
		sourceUnit = null;
		stack.clear();
	}

	public void visitModule(ModuleNode node) {
		pushASTNode(node);
		try {
			node.getClasses().forEach(classInUnit -> {
				visitClass(classInUnit);
			});
		} finally {
			popASTNode();
		}
	}

	// GroovyClassVisitor

	public void visitClass(ClassNode node) {
		URI uri = sourceUnit.getSource().getURI();
		classNodesByURI.get(uri).add(node);
		pushASTNode(node);
		try {
			ClassNode unresolvedSuperClass = node.getUnresolvedSuperClass();
			if (unresolvedSuperClass != null && unresolvedSuperClass.getLineNumber() != -1) {
				pushASTNode(unresolvedSuperClass);
				popASTNode();
			}
			for (ClassNode unresolvedInterface : node.getUnresolvedInterfaces()) {
				if (unresolvedInterface.getLineNumber() == -1) {
					continue;
				}
				pushASTNode(unresolvedInterface);
				popASTNode();
			}
			super.visitClass(node);
		} finally {
			popASTNode();
		}
	}

	@Override
	public void visitImports(ModuleNode node) {
		if (node != null) {
			for (ImportNode importNode : node.getImports()) {
				pushASTNode(importNode);
				visitAnnotations(importNode);
				importNode.visit(this);
				popASTNode();
			}
			for (ImportNode importStarNode : node.getStarImports()) {
				pushASTNode(importStarNode);
				visitAnnotations(importStarNode);
				importStarNode.visit(this);
				popASTNode();
			}
			for (ImportNode importStaticNode : node.getStaticImports().values()) {
				pushASTNode(importStaticNode);
				visitAnnotations(importStaticNode);
				importStaticNode.visit(this);
				popASTNode();
			}
			for (ImportNode importStaticStarNode : node.getStaticStarImports().values()) {
				pushASTNode(importStaticStarNode);
				visitAnnotations(importStaticStarNode);
				importStaticStarNode.visit(this);
				popASTNode();
			}
		}
	}

	public void visitConstructor(ConstructorNode node) {
		pushASTNode(node);
		try {
			super.visitConstructor(node);
			for (Parameter parameter : node.getParameters()) {
				visitParameter(parameter);
			}
		} finally {
			popASTNode();
		}
	}

	public void visitMethod(MethodNode node) {
		pushASTNode(node);
		try {
			super.visitMethod(node);
			for (Parameter parameter : node.getParameters()) {
				visitParameter(parameter);
			}
		} finally {
			popASTNode();
		}
	}

	protected void visitParameter(Parameter node) {
		pushASTNode(node);
		try {
		} finally {
			popASTNode();
		}
	}

	public void visitField(FieldNode node) {
		pushASTNode(node);
		try {
			super.visitField(node);
		} finally {
			popASTNode();
		}
	}

	public void visitProperty(PropertyNode node) {
		pushASTNode(node);
		try {
			super.visitProperty(node);
		} finally {
			popASTNode();
		}
	}

	// GroovyCodeVisitor

	public void visitBlockStatement(BlockStatement node) {
		pushASTNode(node);
		try {
			super.visitBlockStatement(node);
		} finally {
			popASTNode();
		}
	}

	public void visitForLoop(ForStatement node) {
		pushASTNode(node);
		try {
			super.visitForLoop(node);
		} finally {
			popASTNode();
		}
	}

	public void visitWhileLoop(WhileStatement node) {
		pushASTNode(node);
		try {
			super.visitWhileLoop(node);
		} finally {
			popASTNode();
		}
	}

	public void visitDoWhileLoop(DoWhileStatement node) {
		pushASTNode(node);
		try {
			super.visitDoWhileLoop(node);
		} finally {
			popASTNode();
		}
	}

	public void visitIfElse(IfStatement node) {
		pushASTNode(node);
		try {
			super.visitIfElse(node);
		} finally {
			popASTNode();
		}
	}

	public void visitExpressionStatement(ExpressionStatement node) {
		pushASTNode(node);
		try {
			super.visitExpressionStatement(node);
		} finally {
			popASTNode();
		}
	}

	public void visitReturnStatement(ReturnStatement node) {
		pushASTNode(node);
		try {
			super.visitReturnStatement(node);
		} finally {
			popASTNode();
		}
	}

	public void visitAssertStatement(AssertStatement node) {
		pushASTNode(node);
		try {
			super.visitAssertStatement(node);
		} finally {
			popASTNode();
		}
	}

	public void visitTryCatchFinally(TryCatchStatement node) {
		pushASTNode(node);
		try {
			super.visitTryCatchFinally(node);
		} finally {
			popASTNode();
		}
	}

	public void visitEmptyStatement(EmptyStatement node) {
		pushASTNode(node);
		try {
			super.visitEmptyStatement(node);
		} finally {
			popASTNode();
		}
	}

	public void visitSwitch(SwitchStatement node) {
		pushASTNode(node);
		try {
			super.visitSwitch(node);
		} finally {
			popASTNode();
		}
	}

	public void visitCaseStatement(CaseStatement node) {
		pushASTNode(node);
		try {
			super.visitCaseStatement(node);
		} finally {
			popASTNode();
		}
	}

	public void visitBreakStatement(BreakStatement node) {
		pushASTNode(node);
		try {
			super.visitBreakStatement(node);
		} finally {
			popASTNode();
		}
	}

	public void visitContinueStatement(ContinueStatement node) {
		pushASTNode(node);
		try {
			super.visitContinueStatement(node);
		} finally {
			popASTNode();
		}
	}

	public void visitSynchronizedStatement(SynchronizedStatement node) {
		pushASTNode(node);
		try {
			super.visitSynchronizedStatement(node);
		} finally {
			popASTNode();
		}
	}

	public void visitThrowStatement(ThrowStatement node) {
		pushASTNode(node);
		try {
			super.visitThrowStatement(node);
		} finally {
			popASTNode();
		}
	}

	public void visitMethodCallExpression(MethodCallExpression node) {
		pushASTNode(node);
		try {
			super.visitMethodCallExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitStaticMethodCallExpression(StaticMethodCallExpression node) {
		pushASTNode(node);
		try {
			super.visitStaticMethodCallExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitConstructorCallExpression(ConstructorCallExpression node) {
		pushASTNode(node);
		try {
			super.visitConstructorCallExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitBinaryExpression(BinaryExpression node) {
		pushASTNode(node);
		try {
			super.visitBinaryExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitTernaryExpression(TernaryExpression node) {
		pushASTNode(node);
		try {
			super.visitTernaryExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitShortTernaryExpression(ElvisOperatorExpression node) {
		pushASTNode(node);
		try {
			super.visitShortTernaryExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitPostfixExpression(PostfixExpression node) {
		pushASTNode(node);
		try {
			super.visitPostfixExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitPrefixExpression(PrefixExpression node) {
		pushASTNode(node);
		try {
			super.visitPrefixExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitBooleanExpression(BooleanExpression node) {
		pushASTNode(node);
		try {
			super.visitBooleanExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitNotExpression(NotExpression node) {
		pushASTNode(node);
		try {
			super.visitNotExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitClosureExpression(ClosureExpression node) {
		pushASTNode(node);
		try {
			super.visitClosureExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitTupleExpression(TupleExpression node) {
		pushASTNode(node);
		try {
			super.visitTupleExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitListExpression(ListExpression node) {
		pushASTNode(node);
		try {
			super.visitListExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitArrayExpression(ArrayExpression node) {
		pushASTNode(node);
		try {
			super.visitArrayExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitMapExpression(MapExpression node) {
		pushASTNode(node);
		try {
			super.visitMapExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitMapEntryExpression(MapEntryExpression node) {
		pushASTNode(node);
		try {
			super.visitMapEntryExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitRangeExpression(RangeExpression node) {
		pushASTNode(node);
		try {
			super.visitRangeExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitSpreadExpression(SpreadExpression node) {
		pushASTNode(node);
		try {
			super.visitSpreadExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitSpreadMapExpression(SpreadMapExpression node) {
		pushASTNode(node);
		try {
			super.visitSpreadMapExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitMethodPointerExpression(MethodPointerExpression node) {
		pushASTNode(node);
		try {
			super.visitMethodPointerExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitUnaryMinusExpression(UnaryMinusExpression node) {
		pushASTNode(node);
		try {
			super.visitUnaryMinusExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitUnaryPlusExpression(UnaryPlusExpression node) {
		pushASTNode(node);
		try {
			super.visitUnaryPlusExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitBitwiseNegationExpression(BitwiseNegationExpression node) {
		pushASTNode(node);
		try {
			super.visitBitwiseNegationExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitCastExpression(CastExpression node) {
		pushASTNode(node);
		try {
			super.visitCastExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitConstantExpression(ConstantExpression node) {
		pushASTNode(node);
		try {
			super.visitConstantExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitClassExpression(ClassExpression node) {
		pushASTNode(node);
		try {
			super.visitClassExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitVariableExpression(VariableExpression node) {
		pushASTNode(node);
		try {
			super.visitVariableExpression(node);
		} finally {
			popASTNode();
		}
	}

	// this calls visitBinaryExpression()
	// public void visitDeclarationExpression(DeclarationExpression node) {
	// pushASTNode(node);
	// try {
	// super.visitDeclarationExpression(node);
	// } finally {
	// popASTNode();
	// }
	// }

	public void visitPropertyExpression(PropertyExpression node) {
		pushASTNode(node);
		try {
			super.visitPropertyExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitAttributeExpression(AttributeExpression node) {
		pushASTNode(node);
		try {
			super.visitAttributeExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitFieldExpression(FieldExpression node) {
		pushASTNode(node);
		try {
			super.visitFieldExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitGStringExpression(GStringExpression node) {
		pushASTNode(node);
		try {
			super.visitGStringExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitCatchStatement(CatchStatement node) {
		pushASTNode(node);
		try {
			super.visitCatchStatement(node);
		} finally {
			popASTNode();
		}
	}

	// this calls visitTupleListExpression()
	// public void visitArgumentlistExpression(ArgumentListExpression node) {
	// pushASTNode(node);
	// try {
	// super.visitArgumentlistExpression(node);
	// } finally {
	// popASTNode();
	// }
	// }

	public void visitClosureListExpression(ClosureListExpression node) {
		pushASTNode(node);
		try {
			super.visitClosureListExpression(node);
		} finally {
			popASTNode();
		}
	}

	public void visitBytecodeExpression(BytecodeExpression node) {
		pushASTNode(node);
		try {
			super.visitBytecodeExpression(node);
		} finally {
			popASTNode();
		}
	}
}