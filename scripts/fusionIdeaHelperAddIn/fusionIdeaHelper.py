# Copyright 2018, Ben Gruver
#
# Redistribution and use in source and binary forms, with or without modification,
# are permitted provided that the following conditions are met:
#
# 1. Redistributions of source code must retain the above copyright notice, this
# list of conditions and the following disclaimer.
#
# 2. Redistributions in binary form must reproduce the above copyright notice,
# this list of conditions and the following disclaimer in the documentation and/or
# other materials provided with the distribution.
#
# 3. Neither the name of the copyright holder nor the names of its contributors
# may be used to endorse or promote products derived from this software without
# specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
# ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
# ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

"""
When IDEA injects a python script into the Fusion process, it gets run as a
separate thread. However, most things in the Fusion API can't be accessed
off of the maid thread.

In order to get around this, this add-in registers a custom event, which gets
fired by the injection script with the name of the script to run. The event handler
runs on the main thread and runs the script similarly to how Fusion would normally
run it.
"""

import adsk.core
import adsk.fusion
import importlib
import inspect
import json
import os
import sys
import traceback
import types

script_path = os.path.abspath(inspect.getfile(inspect.currentframe()))
script_name = os.path.splitext(os.path.basename(script_path))[0]
script_dir = os.path.dirname(script_path)


# Create a new top-level module so we can share state with the inject script
sys.modules['fusionIdeaHelperGlobals'] = types.ModuleType('fusionIdeaHelperGlobals')
sys.modules['fusionIdeaHelperGlobals'].running = True

custom_event_name = 'fusion_idea_run_script'

app = adsk.core.Application.get()
ui = None
handlers = []
saved_context = None
custom_event = None


class ThreadEventHandler(adsk.core.CustomEventHandler):
    def __init__(self):
        super().__init__()

    def notify(self, args):
        try:
            input = json.loads(args.additionalInfo)
            script_path = os.path.abspath(input['script'])
            detach = input['detach']

            if os.path.isfile(script_path):
                script_name = os.path.splitext(os.path.basename(script_path))[0]
                script_dir = os.path.dirname(script_path)

                sys.path.append(script_dir)
                try:
                    module = importlib.import_module(script_name)
                    importlib.reload(module)
                    module.run(saved_context)
                finally:
                    del sys.path[-1]
                    if detach:
                        try:
                            import pydevd
                            pydevd.stoptrace()
                        except:
                            pass
        except:
            traceback.print_exc()
            if ui:
                ui.messageBox('Failed:\n{}'.format(traceback.format_exc()))


def run(context):
    global ui, app, custom_event, saved_context

    saved_context = context

    try:
        app = adsk.core.Application.get()
        ui = app.userInterface

        custom_event = app.registerCustomEvent(custom_event_name)
        event_handler = ThreadEventHandler()

        custom_event.add(event_handler)
        handlers.append(event_handler)

        sys.modules['fusionIdeaHelperGlobals'].running = True
    except:
        if ui:
            ui.messageBox('Failed:\n{}'.format(traceback.format_exc()))


def stop(context):
    app = adsk.core.Application.get()
    ui = app.userInterface
    try:
        sys.modules['fusionIdeaHelperGlobals'].running = False

        for handler in handlers:
            custom_event.remove(handler)
        app.unregisterCustomEvent(custom_event_name)
    except:
        if ui:
            ui.messageBox('Failed:\n{}'.format(traceback.format_exc()))
