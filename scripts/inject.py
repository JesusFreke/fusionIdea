import inspect
import os
import sys

def process_command_line(argv):
    setup = {}
    setup['port'] = 5678
    setup['pid'] = 0
    setup['script'] = None
    setup['host'] = '127.0.0.1'
    setup['debug'] = 0

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
    return setup

def main(setup):
    import add_code_to_python_process
    show_debug_info_on_target_process = 0

    setup['pydevd_path'] = os.path.dirname(os.path.dirname(inspect.getfile(add_code_to_python_process)))
    setup['helper_path'] = os.path.dirname(inspect.getfile(add_code_to_python_process))

    if sys.platform == 'win32':
        setup['helper_path'] = setup['helper_path'].replace('\\', '/')
        setup['pydevd_path'] = setup['pydevd_path'].replace('\\', '/')
        setup['detach'] = 0

        if setup['script'] and setup['debug']:
            setup['detach'] = 1

        python_code = '''import adsk.core;'''
        if setup['debug']:
            python_code += '''import sys;import time;
sys.path.append("%(helper_path)s");
sys.path.append("%(pydevd_path)s");
import attach_script;
sys.stderr.flush = lambda: None;
sys.stdout.flush = lambda: None;
attach_script.attach(port=%(port)s, host="%(host)s");
sys.stdout.value = "blah";
sys.stderr.value = "blah";
'''
        if setup['script']:
            setup['script'] = setup['script'].replace('\\', '/')
            python_code += '''
import threading;
import json;
adsk.core.Application.get().fireCustomEvent(
    "fusion_idea_run_script", json.dumps({"script": "%(script)s", "detach": %(detach)d}));
'''
    else:
        # We have to pass it a bit differently for gdb
        python_code = '''import adsk.core;'''
        if setup['debug']:
            python_code += '''import sys;
sys.path.append(\\\"%(helper_path)s\\\");
import attach_script;
del sys.path[-1];
attach_script.attach(port=%(port)s, host=\\\"%(host)s\\\");'''
        python_code += '''
adsk.core.Application.get().fireCustomEvent(\\\"fusion_idea_run_script\\\", \\\"%(script)s\\\");
'''
    python_code = python_code % setup

    print(python_code)

    python_code = python_code.replace('\r\n', '').replace('\r', '').replace('\n', '')
    add_code_to_python_process.run_python_code(
            setup['pid'], python_code, connect_debugger_tracing=setup['debug'], show_debug_info=show_debug_info_on_target_process)

if __name__ == '__main__':
    main(process_command_line(sys.argv[1:]))
