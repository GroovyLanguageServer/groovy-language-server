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
import findJava from "./utils/findJava";
import getAdditionalClasspathFolder from "./utils/extClassPath";
import * as path from "path";
import * as vscode from "vscode";


import {
  LanguageClient,
  LanguageClientOptions,
  Executable
} from "vscode-languageclient";

const MISSING_JAVA_ERROR =
  "Could not locate valid JDK. To configure JDK manually, use the groovy.java.home setting.";
const INITIALIZING_MESSAGE = "Initializing Groovy language server...";
const RELOAD_WINDOW_MESSAGE =
  "To apply new settings for Groovy, please reload the window.";
const STARTUP_ERROR = "The Groovy extension failed to start.";
const LABEL_RELOAD_WINDOW = "Reload Window";
let extensionContext: vscode.ExtensionContext;
let languageClient: LanguageClient;
let javaPath: string;

function onDidChangeConfiguration(event: vscode.ConfigurationChangeEvent) {
  if (event.affectsConfiguration("groovy.java.home") ||
  	  event.affectsConfiguration("groovy.additional.libraries")) {
    //we're going to try to kill the language server and then restart
    //it with the new settings
    restartLanguageServer();
  }
}

function restartLanguageServer() {
  if (!languageClient) {
    startLanguageServer();
    return;
  }
  let oldLanguageClient = languageClient;
  languageClient = null;
  oldLanguageClient.stop().then(
    () => {
      startLanguageServer();
    },
    () => {
      //something went wrong restarting the language server...
      //this shouldn't happen, but if it does, the user can manually restart
      vscode.window
        .showWarningMessage(RELOAD_WINDOW_MESSAGE, LABEL_RELOAD_WINDOW)
        .then(action => {
          if (action === LABEL_RELOAD_WINDOW) {
            vscode.commands.executeCommand("workbench.action.reloadWindow");
          }
        });
    }
  );
}

export function activate(context: vscode.ExtensionContext) {
  console.log("Activating Groovy LSP");
  extensionContext = context;
  javaPath = findJava();
  vscode.workspace.onDidChangeConfiguration(onDidChangeConfiguration);

  vscode.commands.registerCommand(
    "groovy.restartServer",
    restartLanguageServer
  );

  startLanguageServer();
}

export function deactivate() {
  console.log("Deactivating Groovy LSP");
  extensionContext = null;
}

function startLanguageServer() {
  if (!extensionContext) {
    //something very bad happened!
    console.log("something very bad happened!");
    return;
  }
  /*if (vscode.workspace.workspaceFolders === undefined) {
    vscode.window
      .showInformationMessage(
        MISSING_WORKSPACE_ROOT_ERROR,
        LABEL_OPEN_FOLDER,
        LABEL_OPEN_WORKSPACE
      )
      .then(action => {
        switch (action) {
          case LABEL_OPEN_WORKSPACE: {
            vscode.commands.executeCommand("workbench.action.openWorkspace");
            break;
          }
          case LABEL_OPEN_FOLDER: {
            vscode.commands.executeCommand("workbench.action.files.openFolder");
            break;
          }
        }
      });
    return;
  }*/
  if (!javaPath) {
    vscode.window.showErrorMessage(MISSING_JAVA_ERROR);
    console.log(MISSING_JAVA_ERROR);
    return;
  }

  vscode.window.withProgress(
    { location: vscode.ProgressLocation.Window },
    progress => {
      return new Promise((resolve, reject) => {
		progress.report({ message: INITIALIZING_MESSAGE });
		let extClassPath = getAdditionalClasspathFolder();
        let clientOptions: LanguageClientOptions = {
		  documentSelector: [{ scheme: "file", language: "groovy" }],
		  initializationOptions: [{
			"ADDITIONAL_CLASSPATH_FOLDER": `${extClassPath}`
		  }],
          synchronize: {
            configurationSection: "groovy"
          },
          uriConverters: {
            code2Protocol: (value: vscode.Uri) => {
              if (/^win32/.test(process.platform)) {
                //drive letters on Windows are encoded with %3A instead of :
                //but Java doesn't treat them the same
                return value.toString().replace("%3A", ":");
              } else {
                return value.toString();
              }
            },
            //this is just the default behavior, but we need to define both
            protocol2Code: value => vscode.Uri.parse(value)
          }
		};
        let args = [
          "-jar",
          path.resolve(extensionContext.extensionPath, "bin", "groovy-language-server-all.jar")
        ];
        //uncomment to allow a debugger to attach to the language server
        args.unshift("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005,quiet=y");
        let executable: Executable = {
          command: javaPath,
          args: args
        };
        console.log("Groovy LSP Executable: " + javaPath);
        console.log("Groovy LSP args: " + args);

        languageClient = new LanguageClient(
          "groovy",
          "Groovy Language Server",
          executable,
          clientOptions
        );
        languageClient.onReady().then(
          ()=> {},
          error => {
            resolve();
            vscode.window.showErrorMessage(STARTUP_ERROR);
          });
        let disposable = languageClient.start();
        extensionContext.subscriptions.push(disposable);
      });
    }
  );
  console.log("Started Groovy LSP");
}
