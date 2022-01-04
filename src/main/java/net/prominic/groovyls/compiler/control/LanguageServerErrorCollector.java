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
package net.prominic.groovyls.compiler.control;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.ErrorCollector;

/**
 * A special ErrorCollector for language servers that can clear all errors and
 * does not throw exceptions.
 */
public class LanguageServerErrorCollector extends ErrorCollector {
    private static final long serialVersionUID = 1L;

    public LanguageServerErrorCollector(CompilerConfiguration configuration) {
        super(configuration);
    }

    public void clear() {
        if (errors != null) {
            errors.clear();
        }
        if (warnings != null) {
            warnings.clear();
        }
    }

    @Override
    protected void failIfErrors() throws CompilationFailedException {
        // don't fail
    }
}
