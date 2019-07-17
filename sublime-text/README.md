# Groovy Language Server with Sublime Text

Learn how to configure [Sublime Text](https://www.sublimetext.com/) to add code intelligence for Groovy using the [Groovy Language Server](https://github.com/prominic/groovy-language-server).

1. Install the [LSP package](https://github.com/tomv564/LSP) for Sublime Text.

2. Download the [Groovy Language Server](https://github.com/prominic/groovy-language-server) source code from Github, and build the project using the instructions in the [_README_ file](https://github.com/prominic/groovy-language-server/blob/master/README.md).

3. In Sublime Text, go to the **Preferences** menu → **Package Settings** → **LSP** → **Settings**. This will open the LSP package settings.

4. In _LSP.sublime-settings — User_, add the configuration for the Groovy Language Server:

   ```json
   {
     "clients": {
       "groovy-language-server": {
         "enabled": true,
         "command": [
           "java",
           "-jar",
           "/absolute/path/to/groovy-language-server-all.jar"
         ],
         "languageId": "groovy",
         "syntaxes": ["Packages/Groovy/Groovy.sublime-syntax"],
         "scopes": ["source.groovy"]
       }
     }
   }
   ```

   Be sure to change to _/absolute/path/to/groovy-language-server-all.jar_ to the real location of this _.jar_ file on your computer. It **must** be an absolute path.
