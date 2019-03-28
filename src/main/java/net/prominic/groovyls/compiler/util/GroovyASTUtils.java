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
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCall;
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

    public static ASTNode getDefinition(ASTNode node, boolean strict, ASTNodeVisitor astVisitor) {
        ASTNode parentNode = astVisitor.getParent(node);
        if (node instanceof ExpressionStatement) {
            ExpressionStatement statement = (ExpressionStatement) node;
            node = statement.getExpression();
        }
        if (node instanceof ClassNode) {
            return tryToResolveOriginalClassNode((ClassNode) node, strict, astVisitor);
        } else if (node instanceof ConstructorCallExpression) {
            ConstructorCallExpression callExpression = (ConstructorCallExpression) node;
            return GroovyASTUtils.getMethodFromCallExpression(callExpression, astVisitor);
        } else if (node instanceof DeclarationExpression) {
            DeclarationExpression declExpression = (DeclarationExpression) node;
            if (!declExpression.isMultipleAssignmentDeclaration()) {
                ClassNode originType = declExpression.getVariableExpression().getOriginType();
                return tryToResolveOriginalClassNode(originType, strict, astVisitor);
            }
        } else if (node instanceof ClassExpression) {
            ClassExpression classExpression = (ClassExpression) node;
            return tryToResolveOriginalClassNode(classExpression.getType(), strict, astVisitor);
        } else if (node instanceof MethodNode) {
            return node;
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
        ASTNode definitionNode = getDefinition(node, false, astVisitor);
        if (definitionNode == null) {
            return null;
        }
        if (definitionNode instanceof MethodNode) {
            MethodNode method = (MethodNode) definitionNode;
            return tryToResolveOriginalClassNode(method.getReturnType(), true, astVisitor);
        } else if (definitionNode instanceof Variable) {
            Variable variable = (Variable) definitionNode;
            return tryToResolveOriginalClassNode(variable.getOriginType(), true, astVisitor);
        }
        return null;
    }

    public static List<ASTNode> getReferences(ASTNode node, ASTNodeVisitor ast) {
        ASTNode definitionNode = getDefinition(node, true, ast);
        if (definitionNode == null) {
            return Collections.emptyList();
        }
        return ast.getNodes().stream().filter(otherNode -> {
            ASTNode otherDefinition = getDefinition(otherNode, false, ast);
            return definitionNode.equals(otherDefinition) && node.getLineNumber() != -1 && node.getColumnNumber() != -1;
        }).collect(Collectors.toList());
    }

    private static ClassNode tryToResolveOriginalClassNode(ClassNode node, boolean strict, ASTNodeVisitor ast) {
        for (ClassNode originalNode : ast.getClassNodes()) {
            if (originalNode.equals(node)) {
                return originalNode;
            }
        }
        if (strict) {
            return null;
        }
        return node;
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

    public static List<PropertyNode> getPropertiesForLeftSideOfPropertyExpression(Expression node,
            ASTNodeVisitor astVisitor) {
        if (node instanceof ClassExpression) {
            ClassExpression expression = (ClassExpression) node;
            // This means it's an expression like this: SomeClass.someProp
            return expression.getType().getProperties();
        } else if (node instanceof VariableExpression) {
            // function called on instance of some class
            VariableExpression var = (VariableExpression) node;
            if (var.getName().equals("this")) {
                ClassNode enclosingClass = getEnclosingClass(node, astVisitor);
                if (enclosingClass != null) {
                    return enclosingClass.getProperties();
                }
            } else if (var.getOriginType() != null) {
                return var.getOriginType().getProperties();
            }
        }
        return Collections.emptyList();
    }

    public static List<MethodNode> getMethodsForLeftSideOfPropertyExpression(Expression node,
            ASTNodeVisitor astVisitor) {
        if (node instanceof ClassExpression) {
            ClassExpression expression = (ClassExpression) node;
            // This means it's an expression like this: SomeClass.someProp
            return expression.getType().getMethods();
        } else if (node instanceof VariableExpression) {
            // function called on instance of some class
            VariableExpression var = (VariableExpression) node;
            if (var.getName().equals("this")) {
                ClassNode enclosingClass = getEnclosingClass(node, astVisitor);
                if (enclosingClass != null) {
                    return enclosingClass.getMethods();
                }
            } else if (var.getOriginType() != null) {
                return var.getOriginType().getMethods();
            }
        }
        return Collections.emptyList();
    }

    public static List<MethodNode> getMethodOverloadsFromCallExpression(MethodCall node, ASTNodeVisitor astVisitor) {
        if (node instanceof MethodCallExpression) {
            MethodCallExpression methodCallExpr = (MethodCallExpression) node;
            if (methodCallExpr.getObjectExpression() instanceof ClassExpression) {
                ClassExpression expression = (ClassExpression) methodCallExpr.getObjectExpression();
                // This means it's an expression like this: SomeClass.someMethod
                return expression.getType().getMethods(methodCallExpr.getMethod().getText());
            } else if (methodCallExpr.getObjectExpression() instanceof ConstructorCallExpression) {
                ConstructorCallExpression expression = (ConstructorCallExpression) methodCallExpr.getObjectExpression();
                // Local function, no class used (or technically this used).
                return expression.getType().getMethods(methodCallExpr.getMethod().getText());
            } else if (methodCallExpr.getObjectExpression() instanceof VariableExpression) {
                // function called on instance of some class
                VariableExpression var = (VariableExpression) methodCallExpr.getObjectExpression();
                if (var.getName().equals("this")) {
                    ClassNode enclosingClass = getEnclosingClass(methodCallExpr, astVisitor);
                    if (enclosingClass != null) {
                        return enclosingClass.getMethods(methodCallExpr.getMethod().getText());
                    }
                } else if (var.getOriginType() != null) {
                    return var.getOriginType().getMethods(methodCallExpr.getMethod().getText());
                }
            }
        } else if (node instanceof ConstructorCallExpression) {
            ConstructorCallExpression constructorCallExpr = (ConstructorCallExpression) node;
            ClassNode constructorType = constructorCallExpr.getType();
            if (constructorType != null) {
                return constructorType.getDeclaredConstructors().stream().map(constructor -> (MethodNode) constructor)
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    public static MethodNode getMethodFromCallExpression(MethodCall node, ASTNodeVisitor astVisitor) {
        return getMethodFromCallExpression(node, astVisitor, -1);
    }

    public static MethodNode getMethodFromCallExpression(MethodCall node, ASTNodeVisitor astVisitor, int argIndex) {
        List<MethodNode> possibleMethods = getMethodOverloadsFromCallExpression(node, astVisitor);
        if (!possibleMethods.isEmpty() && node.getArguments() instanceof ArgumentListExpression) {
            ArgumentListExpression actualArguments = (ArgumentListExpression) node.getArguments();
            MethodNode foundMethod = possibleMethods.stream().max(new Comparator<MethodNode>() {
                public int compare(MethodNode m1, MethodNode m2) {
                    Parameter[] p1 = m1.getParameters();
                    Parameter[] p2 = m2.getParameters();
                    int m1Value = calculateArgumentsScore(p1, actualArguments, argIndex);
                    int m2Value = calculateArgumentsScore(p2, actualArguments, argIndex);
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

    private static int calculateArgumentsScore(Parameter[] parameters, ArgumentListExpression arguments, int argIndex) {
        int score = 0;
        int paramCount = parameters.length;
        int expressionsCount = arguments.getExpressions().size();
        int argsCount = expressionsCount;
        if (argIndex >= argsCount) {
            argsCount = argIndex + 1;
        }
        int minCount = Math.min(paramCount, argsCount);
        for (int i = 0; i < minCount; i++) {
            ClassNode argType = (i < expressionsCount) ? arguments.getExpression(i).getType() : null;
            ClassNode paramType = (i < paramCount) ? parameters[i].getType() : null;
            if (argType != null && paramType != null) {
                if (argType.equals(paramType)) {
                    // equal types are preferred
                    score += 1000;
                } else if (argType.isDerivedFrom(paramType)) {
                    // subtypes are nice, but less important
                    score += 100;
                } else {
                    // if a type doesn't match at all, it's not worth much
                    score++;
                }
            } else if (paramType != null) {
                //extra parameters are like a type not matching
                score++;
            }
        }
        return score;
    }
}