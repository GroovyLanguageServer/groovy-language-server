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
import findJava from "./utils/findJava";
import * as path from "path";
import * as vscode from "vscode";
import * as net from "net";
import {
  LanguageClient,
  LanguageClientOptions,
  Executable,
  ServerOptions,
  StreamInfo,
} from "vscode-languageclient/node";

const MISSING_JAVA_ERROR =
  "Could not locate valid JDK. To configure JDK manually, use the groovy.java.home setting.";
const INVALID_JAVA_ERROR =
  "The groovy.java.home setting does not point to a valid JDK.";
const INITIALIZING_MESSAGE = "Initializing Groovy language server...";
const RELOAD_WINDOW_MESSAGE =
  "To apply new settings for Groovy, please reload the window.";
const STARTUP_ERROR = "The Groovy extension failed to start.";
const LABEL_RELOAD_WINDOW = "Reload Window";
let extensionContext: vscode.ExtensionContext | null = null;
let languageClient: LanguageClient | null = null;
let javaPath: string | null = null;

export function activate(context: vscode.ExtensionContext) {
  extensionContext = context;
  javaPath = findJava();

  vscode.workspace.onDidChangeConfiguration(onDidChangeConfiguration);

  vscode.commands.registerCommand(
    "groovy.restartServer",
    restartLanguageServer
  );

  startLanguageServer();
}

export function deactivate(): Thenable<void> | undefined {
  if (!languageClient) {
    return undefined;
  }
  return languageClient.stop();
}

function onDidChangeConfiguration(event: vscode.ConfigurationChangeEvent) {
  if (
    event.affectsConfiguration("groovy.java.home") ||
    event.affectsConfiguration("groovy.debug.serverPort")
  ) {
    javaPath = findJava();
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
        .then((action) => {
          if (action === LABEL_RELOAD_WINDOW) {
            vscode.commands.executeCommand("workbench.action.reloadWindow");
          }
        });
    }
  );
}

function startLanguageServer() {
  vscode.window.withProgress(
    { location: vscode.ProgressLocation.Window },
    (progress) => {
      return new Promise<void>(async (resolve, reject) => {
        if (!extensionContext) {
          resolve();
          vscode.window.showErrorMessage(STARTUP_ERROR);
          return;
        }

        const config = vscode.workspace.getConfiguration("groovy");
        const port = config.get<number>("debug.serverPort") ?? 0;

        progress.report({message: INITIALIZING_MESSAGE});

        let serverOptions: ServerOptions;

        if (port > 0) {
          // === Debug mode: connect to running server ===
          serverOptions = () => {
            return new Promise<StreamInfo>((resolve, reject) => {
              const socket = new net.Socket();
              socket.connect(port, "127.0.0.1", () => {
                console.log(`Connected to Groovy LSP on port ${port}`);
                resolve({reader: socket, writer: socket});
              });
              socket.on("error", reject);
            });
          };
        } else {
          // === Normal mode: launch Java process ===
          if (!javaPath) {
            resolve();
            let settingsJavaHome = config.get<string>("java.home");
            if (settingsJavaHome) {
              vscode.window.showErrorMessage(INVALID_JAVA_ERROR);
            } else {
              vscode.window.showErrorMessage(MISSING_JAVA_ERROR);
            }
            return;
          }

          const args = [
            "-jar",
            path.resolve(
              extensionContext.extensionPath,
              "bin",
              "groovy-language-server-all.jar"
            ),
          ];

          //uncomment to allow a debugger to attach to the language server
          //args.unshift("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005,quiet=y");
          let executable: Executable = {
            command: javaPath,
            args,
          };

          serverOptions = executable;
        }

        let clientOptions: LanguageClientOptions = {
          documentSelector: [{ scheme: "file", language: "groovy" }],
          synchronize: {
            configurationSection: "groovy",
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
            protocol2Code: (value) => vscode.Uri.parse(value),
          },
        };

        languageClient = new LanguageClient(
          "groovy",
          "Groovy Language Server",
          serverOptions,
          clientOptions
        );

        try {
          await languageClient.start();
        } catch (e) {
          vscode.window.showErrorMessage(STARTUP_ERROR);
        }

        resolve();
      });
    }
  );
}
