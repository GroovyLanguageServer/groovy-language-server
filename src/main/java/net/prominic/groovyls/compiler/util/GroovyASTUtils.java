////////////////////////////////////////////////////////////////////////////////
// Copyright 2016 Prominic.NET, Inc.
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
package net.prominic.groovyls.compiler.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;

import net.prominic.groovyls.compiler.ast.ASTNodeVisitor;

public class GroovyASTUtils {
    public static ClassNode getEnclosingClass(ASTNode node, ASTNodeVisitor astVisitor) {
        ASTNode current = node;
        while (current != null) {
            if (current instanceof ClassNode) {
                return (ClassNode) current;
            }
            current = astVisitor.getParent(current);
        }
        return null;
    }

    public static ASTNode getDefinition(ASTNode node, ASTNodeVisitor astVisitor) {
        ASTNode parentNode = astVisitor.getParent(node);
        if (node instanceof ExpressionStatement) {
            ExpressionStatement statement = (ExpressionStatement) node;
            node = statement.getExpression();
        }
        if (node instanceof ClassNode || node instanceof MethodNode) {
            return node;
        } else if (node instanceof ConstructorCallExpression) {
            ConstructorCallExpression callExpression = (ConstructorCallExpression) node;
            return resolveOriginalClassNode(callExpression.getType(), astVisitor);
        } else if (node instanceof DeclarationExpression) {
            DeclarationExpression declExpression = (DeclarationExpression) node;
            if (!declExpression.isMultipleAssignmentDeclaration()) {
                ClassNode originType = declExpression.getVariableExpression().getOriginType();
                return resolveOriginalClassNode(originType, astVisitor);
            }
        } else if (node instanceof ClassExpression) {
            ClassExpression classExpression = (ClassExpression) node;
            return resolveOriginalClassNode(classExpression.getType(), astVisitor);
        } else if (node instanceof ConstantExpression && parentNode != null) {
            if (parentNode instanceof MethodCallExpression) {
                MethodCallExpression methodCallExpression = (MethodCallExpression) parentNode;
                return GroovyASTUtils.getMethodFromCallExpression(methodCallExpression, astVisitor);
            } else if (parentNode instanceof PropertyExpression) {
                PropertyExpression propertyExpression = (PropertyExpression) parentNode;
                return GroovyASTUtils.getPropertyFromExpression(propertyExpression, astVisitor);
            }
        } else if (node instanceof VariableExpression) {
            VariableExpression variableExpression = (VariableExpression) node;
            Variable accessedVariable = variableExpression.getAccessedVariable();
            if (accessedVariable instanceof ASTNode) {
                return (ASTNode) accessedVariable;
            }
            // DynamicVariable is not an ASTNode, so skip it
            return null;
        } else if (node instanceof Variable) {
            return node;
        }
        return null;
    }

    public static ASTNode getTypeDefinition(ASTNode node, ASTNodeVisitor astVisitor) {
        ASTNode definitionNode = getDefinition(node, astVisitor);
        if (definitionNode == null) {
            return null;
        }
        if (definitionNode instanceof MethodNode) {
            MethodNode method = (MethodNode) definitionNode;
            return resolveOriginalClassNode(method.getReturnType(), astVisitor);
        } else if (definitionNode instanceof Variable) {
            Variable variable = (Variable) definitionNode;
            return resolveOriginalClassNode(variable.getOriginType(), astVisitor);
        }
        return null;
    }

    public static List<ASTNode> getReferences(ASTNode node, ASTNodeVisitor ast) {
        ASTNode definitionNode = getDefinition(node, ast);
        if (definitionNode == null) {
            return Collections.emptyList();
        }
        return ast.getNodes().stream().filter(otherNode -> {
            ASTNode otherDefinition = getDefinition(otherNode, ast);
            return definitionNode.equals(otherDefinition) && node.getLineNumber() != -1 && node.getColumnNumber() != -1;
        }).collect(Collectors.toList());
    }

    public static ClassNode resolveOriginalClassNode(ClassNode node, ASTNodeVisitor ast) {
        for (ClassNode originalNode : ast.getClassNodes()) {
            if (originalNode.equals(node)) {
                return originalNode;
            }
        }
        return null;
    }

    public static PropertyNode getPropertyFromExpression(PropertyExpression node, ASTNodeVisitor astVisitor) {
        PropertyNode result = null;
        if (node.getObjectExpression() instanceof ClassExpression) {
            ClassExpression expression = (ClassExpression) node.getObjectExpression();
            // This means it's an expression like this: SomeClass.someMethod
            result = expression.getType().getProperty(node.getProperty().getText());
        } else if (node.getObjectExpression() instanceof ConstructorCallExpression) {
            ConstructorCallExpression expression = (ConstructorCallExpression) node.getObjectExpression();
            // Local function, no class used (or technically this used).
            result = expression.getType().getProperty(node.getProperty().getText());
        } else if (node.getObjectExpression() instanceof VariableExpression) {
            // function called on instance of some class
            VariableExpression var = (VariableExpression) node.getObjectExpression();
            if (var.getName().equals("this")) {
                ClassNode enclosingClass = getEnclosingClass(node, astVisitor);
                if (enclosingClass != null) {
                    result = enclosingClass.getProperty(node.getProperty().getText());
                }
            } else if (var.getOriginType() != null) {
                result = var.getOriginType().getProperty(node.getProperty().getText());
            }
        }
        return result;
    }

    public static List<MethodNode> getMethodOverloadsFromCallExpression(MethodCallExpression node,
            ASTNodeVisitor astVisitor) {
        if (node.getObjectExpression() instanceof ClassExpression) {
            ClassExpression expression = (ClassExpression) node.getObjectExpression();
            // This means it's an expression like this: SomeClass.someMethod
            return expression.getType().getMethods(node.getMethod().getText());
        } else if (node.getObjectExpression() instanceof ConstructorCallExpression) {
            ConstructorCallExpression expression = (ConstructorCallExpression) node.getObjectExpression();
            // Local function, no class used (or technically this used).
            return expression.getType().getMethods(node.getMethod().getText());
        } else if (node.getObjectExpression() instanceof VariableExpression) {
            // function called on instance of some class
            VariableExpression var = (VariableExpression) node.getObjectExpression();
            if (var.getName().equals("this")) {
                ClassNode enclosingClass = getEnclosingClass(node, astVisitor);
                if (enclosingClass != null) {
                    return enclosingClass.getMethods(node.getMethod().getText());
                }
            } else if (var.getOriginType() != null) {
                return var.getOriginType().getMethods(node.getMethod().getText());
            }
        }
        return Collections.emptyList();
    }

    public static MethodNode getMethodFromCallExpression(MethodCallExpression node, ASTNodeVisitor astVisitor) {
        List<MethodNode> possibleMethods = getMethodOverloadsFromCallExpression(node, astVisitor);
        if (!possibleMethods.isEmpty() && node.getArguments() instanceof ArgumentListExpression) {
            ArgumentListExpression actualArguments = (ArgumentListExpression) node.getArguments();
            MethodNode foundMethod = possibleMethods.stream().max(new Comparator<MethodNode>() {
                public int compare(MethodNode m1, MethodNode m2) {
                    int m1Value = calculateArgumentsScore(m1.getParameters(), actualArguments);
                    int m2Value = calculateArgumentsScore(m2.getParameters(), actualArguments);
                    if (m1Value > m2Value) {
                        return 1;
                    } else if (m1Value < m2Value) {
                        return -1;
                    }
                    return 0;
                }
            }).orElse(null);
            return foundMethod;
        }
        return null;
    }

    private static int calculateArgumentsScore(Parameter[] parameters, ArgumentListExpression arguments) {
        int score = 0;
        int paramCount = parameters.length;
        int argsCount = arguments.getExpressions().size();
        if (paramCount == arguments.getExpressions().size()) {
            score += 100;
        }
        if (paramCount >= argsCount) {
            int minCount = Math.min(paramCount, argsCount);
            for (int i = 0; i < minCount; i++) {
                // If they aren't the same type, return false
                ClassNode argType = arguments.getExpression(i).getType();
                ClassNode paramType = parameters[i].getType();
                if (argType.equals(paramType)) {
                    // equal types are preferred
                    score += 10;
                } else if (argType.isDerivedFrom(paramType)) {
                    // subtypes are nice, but less important
                    score++;
                } else {
                    // if a type doesn't match at all, stop checking the rest
                    break;
                }
            }
        }
        return score;
    }
}