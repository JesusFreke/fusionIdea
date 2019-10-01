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

import inspect
import os
import sys

def process_command_line(argv):
    setup = {
        'port': 5678,
        'pid': 0,
        'script': None,
        'host': '127.0.0.1',
        'debug': 0}

    # This script gets run in essentially 3 modes. Debug, Run, and attach.
    # For attach mode, the script argument will be unset. And the debug argument
    # differentiates between debug and run modes.

    i = 0
    while i < len(argv):
        if argv[i] == '--port':
            del argv[i]
            setup['port'] = int(argv[i])
            del argv[i]
        elif argv[i] == '--pid':
            del argv[i]
            setup['pid'] = int(argv[i])
            del argv[i]
        elif argv[i] == '--script':
            del argv[i]
            setup['script'] = argv[i]
            del argv[i]
        elif argv[i] == '--debug':
            del argv[i]
            setup['debug'] = int(argv[i])
            del argv[i]
        else:
            sys.stderr.write('Got unexpected parameter: %s.\n' % argv[i])
            del argv[i]

    if not setup['pid']:
        sys.stderr.write('Expected --pid to be passed.\n')
        sys.exit(1)
    if not setup['debug'] and not setup['script']:
        sys.stderr.write('Expected either --debug to be true or --script to be set.\n')
        sys.exit(1)
    return setup


def main(setup):
    import add_code_to_python_process
    show_debug_info_on_target_process = 0

    script_path = os.path.abspath(inspect.getfile(inspect.currentframe()))
    script_name = os.path.splitext(os.path.basename(script_path))[0]
    script_dir = os.path.dirname(script_path)

    setup['pydevd_path'] = os.path.dirname(os.path.dirname(inspect.getfile(add_code_to_python_process)))
    setup['helper_path'] = os.path.dirname(inspect.getfile(add_code_to_python_process))
    setup['script_path'] = script_dir

    setup['detach'] = 0
    if setup['script'] and setup['debug']:
        setup['detach'] = 1

    if sys.platform == 'win32':
        python_code = ''
        setup['script_path'] = setup['script_path'].replace('\\', '/')
        if setup['debug']:
            setup['helper_path'] = setup['helper_path'].replace('\\', '/')
            setup['pydevd_path'] = setup['pydevd_path'].replace('\\', '/')

            python_code += '''
import os
import sys
sys.path.append("%(helper_path)s")
sys.path.append("%(pydevd_path)s")
os.environ["PYDEVD_USE_FRAME_EVAL"] = "NO"
try:
    import attach_script
    attach_script.attach(port=%(port)s, host="%(host)s")
    import _pydevd_bundle.pydevd_comm
    _pydevd_bundle.pydevd_comm.MAX_IO_MSG_SIZE = 131072
finally:
    del sys.path[-1]
    del sys.path[-1]
'''
        if setup['script']:
            setup['script'] = setup['script'].replace('\\', '/')
            python_code += '''
import adsk.core
import json
import sys
sys.path.append("%(script_path)s")
try:
    import fusion_idea_helper
finally:
    del sys.path[-1]
adsk.core.Application.get().fireCustomEvent(
    "fusion_idea_run_script", json.dumps({"script": "%(script)s", "detach": %(detach)d}))
'''
    else:
        # We have to pass it a bit differently for gdb
        python_code = ''
        if setup['debug']:
            python_code += '''
import sys
sys.path.append(\\\"%(helper_path)s\\\")
sys.path.append(\\\"%(pydevd_path)s\\\")
try:
    import attach_script
    attach_script.attach(port=%(port)s, host=\\\"%(host)s\\\")
    import _pydevd_bundle.pydevd_comm
    _pydevd_bundle.pydevd_comm.MAX_IO_MSG_SIZE = 131072
finally
    del sys.path[-1]
    del sys.path[-1]
'''
        if setup['script']:
            setup['script'] = setup['script'].replace('\\', '/')
            python_code += '''
import adsk.core
import json
import sys
sys.path.append(\\\"%(script_path)s\\\")
try:
    import fusion_idea_helper
finally:
    del sys.path[-1]
adsk.core.Application.get().fireCustomEvent(
    \\\"fusion_idea_run_script\\\", json.dumps({\\\"script\\\": \\\"%(script)s\\\", \\\"detach\\\": %(detach)d}))
'''

    python_code = python_code % setup
    add_code_to_python_process.run_python_code(
        setup['pid'], python_code, connect_debugger_tracing=setup['debug'],
        show_debug_info=show_debug_info_on_target_process)


if __name__ == '__main__':
    main(process_command_line(sys.argv[1:]))
