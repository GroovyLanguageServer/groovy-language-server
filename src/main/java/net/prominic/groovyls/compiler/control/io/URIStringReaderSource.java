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
package net.prominic.groovyls.compiler.control.io;

import java.net.URI;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.io.StringReaderSource;

public class URIStringReaderSource extends StringReaderSource {
	private final URI uri;

	public URIStringReaderSource(String string, URI uri, CompilerConfiguration configuration) {
		super(string, configuration);
		this.uri = uri;
	}

	public URI getURI() {
		return uri;
	}
}