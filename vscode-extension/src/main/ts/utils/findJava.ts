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
import * as fs from "fs";
import * as path from "path";
import * as vscode from "vscode";

export default function findJava(): string | null {
  var executableFile: string = "java";
  if (process["platform"] === "win32") {
    executableFile += ".exe";
  }

  let settingsJavaHome = vscode.workspace
    .getConfiguration("groovy")
    .get("java.home") as string;
  if (settingsJavaHome) {
    let javaPath = path.join(settingsJavaHome, "bin", executableFile);
    if (validate(javaPath)) {
      return javaPath;
    }
    //if the user specified java home in the settings, try no fallbacks...
    //it is confusing if something works with an invalid path
    return null;
  }

  if ("JAVA_HOME" in process.env) {
    let javaHome = process.env.JAVA_HOME as string;
    let javaPath = path.join(javaHome, "bin", executableFile);
    if (validate(javaPath)) {
      return javaPath;
    }
  }

  if ("PATH" in process.env) {
    let PATH = process.env.PATH as string;
    let paths = PATH.split(path.delimiter);
    let pathCount = paths.length;
    for (let i = 0; i < pathCount; i++) {
      let javaPath = path.join(paths[i], executableFile);
      if (validate(javaPath)) {
        return javaPath;
      }
    }
  }

  return null;
}

function validate(javaPath: string): boolean {
  return fs.existsSync(javaPath) && fs.statSync(javaPath).isFile();
}
