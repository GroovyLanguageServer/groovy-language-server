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
package net.prominic.groovyls.compiler.control;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.ast.CompileUnit;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;

public class GroovyLSCompilationUnit extends CompilationUnit {
    public GroovyLSCompilationUnit(CompilerConfiguration config) {
        super(config);
        this.errorCollector = new LanguageServerErrorCollector(config);
    }

    public void configure(CompilerConfiguration configuration) {
        super.configure(configuration);
        this.debug = configuration.getDebug();
        this.appendCompilerConfigurationClasspathToClassLoader(configuration, this.classLoader);
        this.configured = true;
    }

    private void appendCompilerConfigurationClasspathToClassLoader(CompilerConfiguration configuration, GroovyClassLoader classLoader) {
        for (Iterator<String> iterator = configuration.getClasspath().iterator(); iterator.hasNext(); ) {
            classLoader.addClasspath(iterator.next());
        }
    }

    public void setErrorCollector(LanguageServerErrorCollector errorCollector) {
        this.errorCollector = errorCollector;
    }

    public void removeSources(Collection<SourceUnit> sourceUnitsToRemove) {
        for (SourceUnit sourceUnit : sourceUnitsToRemove) {
            if (sourceUnit.getAST() != null) {
                List<String> sourceUnitClassNames = sourceUnit.getAST().getClasses().stream()
                        .map(classNode -> classNode.getName()).collect(Collectors.toList());
                generatedClasses.removeIf(groovyClass -> sourceUnitClassNames.contains(groovyClass.getName()));
                for (String className : sourceUnitClassNames) {
                    summariesByPublicClassName.remove(className);
                    classSourcesByPublicClassName.remove(className);
                }
            }

            summariesBySourceName.remove(sourceUnit.getName());
            sources.remove(sourceUnit.getName());
            names.remove(sourceUnit.getName());
        }
        //keep existing modules from other source units
        List<ModuleNode> modules = ast.getModules();
        ast = new CompileUnit(this.classLoader, null, this.configuration);
        for (ModuleNode module : modules) {
            if (!sourceUnitsToRemove.contains(module.getContext())) {
                ast.addModule(module);
            }
        }
        LanguageServerErrorCollector lsErrorCollector = (LanguageServerErrorCollector) errorCollector;
        lsErrorCollector.clear();
    }

    public void removeSource(SourceUnit sourceUnit) {
        removeSources(Collections.singletonList(sourceUnit));
    }
}