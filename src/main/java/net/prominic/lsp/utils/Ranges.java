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
package net.prominic.lsp.utils;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

public class Ranges {
	public static boolean contains(Range range, Position position) {
		return Positions.COMPARATOR.compare(position, range.getStart()) >= 0
				&& Positions.COMPARATOR.compare(position, range.getEnd()) <= 0;
	}

	public static boolean intersect(Range r1, Range r2) {
		return contains(r1, r2.getStart()) || contains(r1, r2.getEnd());
	}
}