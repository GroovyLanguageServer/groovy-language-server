# Groovy Language Server

A [language server](https://microsoft.github.io/language-server-protocol/) for [Groovy](http://groovy-lang.org/).

The following language server protocol requests are currently supported:

- completion
- definition
- documentSymbol
- hover
- references
- rename
- signatureHelp
- symbol
- typeDefinition

The following configuration options are supported:

- groovy.java.home (`string` - sets a custom JDK path)
- groovy.classpath (`string[]` - sets a custom classpath to include _.jar_ files)

## Build

To build from the command line, run the following command:

```sh
./gradlew build
```

This will create _build/libs/groovy-language-server-all.jar_.

## Run

To run the language server, use the following command:

```sh
java -jar groovy-language-server-all.jar
```

Language server protocol messages are passed using standard I/O.

## Editors and IDEs

A sample language extension for Visual Studio Code is available in the _vscode-extension_ directory. There are no plans to release this extension to the VSCode Marketplace at this time.

Instructions for setting up the language server in Sublime Text is available in the _sublime-text_ directory.

Moonshine IDE natively provides a Grails project type that automatically configures the language server.
