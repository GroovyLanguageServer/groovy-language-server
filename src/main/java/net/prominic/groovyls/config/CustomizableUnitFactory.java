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
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;

import net.prominic.groovyls.compiler.control.GroovyLSCompilationUnit;
import net.prominic.groovyls.compiler.control.io.StringReaderSourceWithURI;
import net.prominic.groovyls.util.FileContentsTracker;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import groovy.util.DelegatingScript;

public class CustomizableUnitFactory implements ICompilationUnitFactory {
	private static final String FILE_EXTENSION_GROOVY = ".groovy";

	private GroovyLSCompilationUnit compilationUnit;
	private List<File> additionalLibrariesFolders = new ArrayList<>();

	public CustomizableUnitFactory() {
	}

	protected void addLibraries(CompilerConfiguration config) {
		ArrayList<String> libraries = new ArrayList<>();
		for (File folder : additionalLibrariesFolders) {
			File[] jars = folder.listFiles(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".jar");
				}
			});
			for (File jar : jars) {
				if (!libraries.contains(jar.getPath())) {
					libraries.add(jar.getPath());
				}
			}
		}
		config.setClasspathList(libraries);
	}

	public void invalidateCompilationUnit() {
		compilationUnit = null;
	}

	public void setAdditionalLibsFolders(String path) {
		this.additionalLibrariesFolders.clear();
		if (path != null) {
			String[] parts = path.split(";");
			for (String part : parts) {
				File f = new File(part.trim());
				if (f.exists() && f.isDirectory() && f.canRead()) {
					this.additionalLibrariesFolders.add(f);
				}
			}
		}
		invalidateCompilationUnit();
	}

	public GroovyLSCompilationUnit create(Path workspaceRoot, FileContentsTracker fileContentsTracker) {
		Set<URI> changedUris = fileContentsTracker.getChangedURIs();

		if (compilationUnit == null) {
			CompilerConfiguration config = newCustomizableCompilerConfiguration();
			compilationUnit = new GroovyLSCompilationUnit(config);
			//we don't care about changed URIs if there's no compilation unit yet
			changedUris = null;
		} else {
			final Set<URI> urisToRemove = changedUris;
			List<SourceUnit> sourcesToRemove = new ArrayList<>();
			compilationUnit.iterator().forEachRemaining(sourceUnit -> {
				URI uri = sourceUnit.getSource().getURI();
				if (urisToRemove.contains(uri)) {
					sourcesToRemove.add(sourceUnit);
				}
			});
			//if an URI has changed, we remove it from the compilation unit so
			//that a new version can be built from the updated source file
			compilationUnit.removeSources(sourcesToRemove);
		}

		if (workspaceRoot != null) {
			addDirectoryToCompilationUnit(workspaceRoot, compilationUnit, fileContentsTracker, changedUris);
		} else {
			final Set<URI> urisToAdd = changedUris;
			fileContentsTracker.getOpenURIs().forEach(uri -> {
				//if we're only tracking changes, skip all files that haven't
				//actually changed
				if (urisToAdd != null && !urisToAdd.contains(uri)) {
					return;
				}
				String contents = fileContentsTracker.getContents(uri);
				addOpenFileToCompilationUnit(uri, contents, compilationUnit);
			});
		}

		return compilationUnit;
	}

	protected CompilerConfiguration newCustomizableCompilerConfiguration() {
		CompilerConfiguration config = new CompilerConfiguration();

		config.setDebug(true);
		config.setVerbose(true);

		addLibraries(config);
		//addScriptBaseClass(config);
		//addCompilationCustomizers(config);

		return config;
	}

	protected void addScriptBaseClass(CompilerConfiguration config) {
		config.setScriptBaseClass(DelegatingScript.class.getName());
	}

	protected void addDirectoryToCompilationUnit(Path dirPath, GroovyLSCompilationUnit compilationUnit,
												 FileContentsTracker fileContentsTracker, Set<URI> changedUris) {
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
							if (changedUris == null || changedUris.contains(fileURI)) {
								compilationUnit.addSource(file);
							}
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
			if (changedUris != null && !changedUris.contains(uri)) {
				return;
			}
			String contents = fileContentsTracker.getContents(uri);
			addOpenFileToCompilationUnit(uri, contents, compilationUnit);
		});
	}

	protected void addOpenFileToCompilationUnit(URI uri, String contents, GroovyLSCompilationUnit compilationUnit) {
		Path filePath = Paths.get(uri);
		SourceUnit sourceUnit = new SourceUnit(filePath.toString(),
				new StringReaderSourceWithURI(contents, uri, compilationUnit.getConfiguration()),
				compilationUnit.getConfiguration(), compilationUnit.getClassLoader(),
				compilationUnit.getErrorCollector());
		compilationUnit.addSource(sourceUnit);
	}
}
