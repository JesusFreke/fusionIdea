### About

This is a plugin for IntelliJ IDEA/PyCharm that facilitates developing Fusion 360 scripts. It supports launching and
debugging scripts in Fusion 360, directly from within IDEA/PyCharm.

This plugin was developed on windows. I would be surprised if it worked on a mac. If someone is interested on adding
support for mac, I would be happy to provide pointers for what may need to be changed.

### Installation
The plugin is available for installation via the built-in plugin repository in IDEA. You can
search for the "Fusion 360 Scripting" plugin.

#### Add-In
Previous versions of the plugin required installation of a small add-in in Fusion 360. This is no longer required for current versions of the plugin.

### Usage

#### IDEA
You enable Fusion 360 support in IDEA by adding the Fusion 360 facet to a module.
See, e.g. Open Module Settings -> Facets. Once you add the facet, take a look at its configuration options
and ensure the path to the Fusion360.exe executable is set correctly.

#### PyCharm
You enable Fusion 360 support for a project in PyCharm in Settings->Languages & Frameworks->Fusion 360.
Enable the "Fusion 360 Support Enabled" checkbox, ensure the path to Fusion360.exe is set correctly,
and then press "Apply".

---

Once support has been enabled, you can write a Fusion 360 script as per usual. Once you are ready to
run it, you can create a new "Fusion 360 Python Script" run configuration, and then run or debug it
as you would expect.

As a convenient shortcut, you can right click on the script in the project browser on the left, or
directly in the editor pane and choose "Run in Fusion 360" or "Debug in Fusion 360" 

### Features
- Run script in Fusion 360
    - launches a script in Fusion 360, as if you had run it from the AddIn window
- Debug script in Fusion 360
    - launches a script in fusion 360 and attached a debugger, letting you stop at breakpoints, and all the usual
    debuggery goodness.
    - Redirects stdout and stderr to the debugging console
- Attach to Process
    - attaches to a Fusion 360 process without running a script. Any breakpoints will be hit if Fusion happens to run
    the breakpointed code. e.g. if you start the script manually in Fusion 360 itself.
- Automatically adds a dependency for the Fusion Python APIs to the module, for autocomplete, contextual docs, etc.
