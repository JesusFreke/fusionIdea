### About

This is a plugin for IntelliJ IDEA/PyCharm that facilitates developing Fusion 360 scripts. It
supports launching and debugging scripts in Fusion 360, directly from within IDEA/PyCharm.

This plugin now supports both Windows and Mac.


**New in v0.6.0 - A Fusion 360 add-in is now required (again) to run/debug scripts. See
[here](https://github.com/JesusFreke/fusion_idea_addin/wiki/Installing-the-add-in-in-Fusion-360) for
more information.**

### Installation
The plugin is available for installation via the built-in plugin repository in IDEA. You can
search for the "Fusion 360 Scripting" plugin.

### Usage

See [here](https://github.com/JesusFreke/fusionIdea/wiki/Getting-started-with-PyCharm-%28Windows%29)
for a step-by-step "Getting Started" guide for PyCharm.

#### IDEA
You enable Fusion 360 support in IDEA by adding the Fusion 360 facet to a module. See, e.g. `Open
Module Settings -> Facets`. Once you add the facet, take a look at its configuration options and
ensure the path to the Fusion 360 executable is set correctly.

#### PyCharm
You enable Fusion 360 support for a project in PyCharm in `Settings->Languages &
Frameworks->Fusion 360`. Enable the "Fusion 360 Support Enabled" checkbox, ensure the path to the
Fusion 360 executable is set correctly, and then press "Apply".

---

Once support has been enabled, you can write a Fusion 360 script as per usual. Once you are ready to
run it, you can create a new "Fusion 360 Python Script" run configuration, and then run or debug it
as you would expect.

As a convenient shortcut, you can right click on the script in the project browser on the left, or
directly in the editor pane and choose "Run in Fusion 360" or "Debug in Fusion 360"

### Features
- Run script in Fusion 360
    - Launches a script in Fusion 360, as if you had run it from the AddIn window.
- Debug script in Fusion 360
    - Launches a script in fusion 360 and attached a debugger, letting you stop at breakpoints, and
      all the usual debuggery goodness.
    - Redirects stdout and stderr to the debugging console.
- Attach to Process
    - Attaches to a Fusion 360 process without running a script. Any breakpoints will be hit if
      Fusion happens to run the breakpointed code. e.g. if you start the script manually in Fusion
      360 itself.
- Fusion 360 Python SDK
    - Adds a new "Fusion 360 Python SDK" type, to simplify creation of a Python SDK pointing to copy
      of Python that is bundled with Fusion.
- Automatically adds a dependency for the Fusion Python APIs to the module, for autocomplete,
  contextual docs, etc.
