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
package net.prominic.groovyls.config;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;

import net.prominic.groovyls.compiler.control.GroovyLSCompilationUnit;
import net.prominic.groovyls.compiler.control.io.StringReaderSourceWithURI;
import net.prominic.groovyls.util.FileContentsTracker;

public class CompilationUnitFactory implements ICompilationUnitFactory {
	private static final String FILE_EXTENSION_GROOVY = ".groovy";
	private static final Path RELATIVE_PATH_SRC_MAIN_GROOVY = Paths.get("src/main/groovy");
	private static final Path RELATIVE_PATH_SRC_TEST_GROOVY = Paths.get("src/test/groovy");

	public CompilationUnitFactory() {
	}

	public GroovyLSCompilationUnit create(Path workspaceRoot, FileContentsTracker fileContentsTracker) {
		CompilerConfiguration config = new CompilerConfiguration();

		Path srcMainGroovyPath = workspaceRoot.resolve(RELATIVE_PATH_SRC_MAIN_GROOVY);
		Path srcTestGroovyPath = workspaceRoot.resolve(RELATIVE_PATH_SRC_TEST_GROOVY);

		GroovyLSCompilationUnit compilationUnit = new GroovyLSCompilationUnit(config);
		addDirectoryToCompilationUnit(srcMainGroovyPath, compilationUnit, fileContentsTracker);
		addDirectoryToCompilationUnit(srcTestGroovyPath, compilationUnit, fileContentsTracker);

		return compilationUnit;
	}

	protected void addDirectoryToCompilationUnit(Path dirPath, GroovyLSCompilationUnit compilationUnit,
			FileContentsTracker fileContentsTracker) {
		try {
			if (Files.exists(dirPath)) {
				Files.walk(dirPath).forEach((filePath) -> {
					if (!filePath.toString().endsWith(FILE_EXTENSION_GROOVY)) {
						return;
					}
					URI fileURI = filePath.toUri();
					if (!fileContentsTracker.isOpen(fileURI)) {
						File file = filePath.toFile();
						if (file.isFile()) {
							compilationUnit.addSource(file);
						}
					}
				});
			}

		} catch (IOException e) {
			System.err.println("Failed to walk directory for source files: " + dirPath);
		}
		fileContentsTracker.getOpenURIs().forEach(uri -> {
			Path openPath = Paths.get(uri);
			if (!openPath.normalize().startsWith(dirPath.normalize())) {
				return;
			}
			String contents = fileContentsTracker.getContents(uri);
			SourceUnit sourceUnit = new SourceUnit(openPath.toString(),
					new StringReaderSourceWithURI(contents, uri, compilationUnit.getConfiguration()),
					compilationUnit.getConfiguration(), compilationUnit.getClassLoader(),
					compilationUnit.getErrorCollector());
			compilationUnit.addSource(sourceUnit);
		});
	}
}