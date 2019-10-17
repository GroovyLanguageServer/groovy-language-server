import * as vscode from "vscode";

export default function getAdditionalClasspathFolder(): string | null {
	return vscode.workspace.getConfiguration("groovy").get("additional.libraries");
}
