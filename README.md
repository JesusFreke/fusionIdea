### About

This is a plugin for IntelliJ IDEA/PyCharm that facilitates developing Fusion 360 scripts. It supports launching and
debugging scripts in Fusion 360, directly from within IDEA.

This plugin was developed on windows. I would be surprised if it worked on a mac. If someone is interested on adding
support for mac, I would be happy to provide pointers for what may need to be changed.

### Installation
The plugin is available for installation via the built-in plugin repository in IDEA. You can
search for the "Fusion 360 Scripting" plugin.

### Usage
You add Fusion 360 support to a module by adding the Fusion 360 facet. See, e.g. Open Module Settings -> Facets.
Once you add the facet, take a look at its configuration options, and ensure the path to the Fusion360.exe executable
is set correctly.

Then, just write a Fusion 360 script as per usual. Once you are ready to run it, you can create a new
"Fusion 360 Python Script" run configuration, and then run or debug it as you would expect.

### Features
- Run script in Fusion 360
    - launches a script in Fusion 360, as if you had run it from the AddIn window
- Debug script in Fusion 360
    - launches a script in fusion 360 and attached a debugger, letting you stop at breakpoints, and all the usual
    debuggery goodness.
- Attach to Process
    - attaches to a Fusion 360 process without running a script. Any breakpoints will be hit if Fusion happens to run
    the breakpointed code. e.g. if you start the script manually in Fusion 360 itself.
- Automatically adds a dependency for the Fusion Python APIs to the module, for autocomplete, contextual docs, etc.
