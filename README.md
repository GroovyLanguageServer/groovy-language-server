# Groovy Language Server

A language server for [Groovy](http://groovy-lang.org/).

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

## Build

To build from the command line, run the following command:

```sh
./gradlew build
```

This will create _build/libs/groovy-language-server-all.jar_.
