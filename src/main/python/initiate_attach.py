# Copyright 2020, Ben Gruver
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

import http.client
import io
import json
import socket
import socketserver
import sys
import time
import traceback
import urllib.request


def process_command_line(argv):
    setup = {
        "port": 5678,
        "pid": 0,
        "script": None,
        "host": "127.0.0.1",
        "debug": 0}

    # This script gets run in essentially 3 modes. Debug, Run, and attach.
    # For attach mode, the script argument will be unset. And the debug argument
    # differentiates between debug and run modes.

    i = 0
    while i < len(argv):
        if argv[i] == "--port":
            del argv[i]
            setup["port"] = int(argv[i])
            del argv[i]
        elif argv[i] == "--pid":
            del argv[i]
            setup["pid"] = int(argv[i])
            del argv[i]
        elif argv[i] == "--script":
            del argv[i]
            setup["script"] = argv[i]
            del argv[i]
        elif argv[i] == "--debug":
            del argv[i]
            setup["debug"] = int(argv[i])
            del argv[i]
        elif argv[i] == "--pydevd_path":
            del argv[i]
            setup["pydevd_path"] = argv[i]
            del argv[i]
        else:
            sys.stderr.write("Got unexpected parameter: %s.\n" % argv[i])
            del argv[i]

    if not setup["pid"]:
        sys.stderr.write("Expected --pid to be passed.\n")
        sys.exit(1)
    if not setup["debug"] and not setup["script"]:
        sys.stderr.write("Expected either --debug to be true or --script to be set.\n")
        sys.exit(1)
    if not setup["pydevd_path"]:
        sys.stderr.write("Expected --pydevd_path to be passed.\n")
        sys.exit(1)
    return setup


class SSDPResponseHandler(socketserver.BaseRequestHandler):

    def handle(self):
        data = self.request[0].strip()

        status_line: bytes
        status_line, headers_text = data.split(b"\r\n", 1)
        headers = http.client.parse_headers(io.BytesIO(headers_text))

        status_atoms = status_line.split(b" ", 3)
        status = status_atoms[1]

        if status != b"200":
            return

        if (headers["ST"] == "fusion_idea:debug" and
                headers["USN"] == "pid:%d" % self.server.target_pid):

            (host, port) = headers["Location"].split(":")
            if host != "127.0.0.1":
                return

            self.server.fusion_debug_port = port


class SSDPClient(socketserver.UDPServer):

    def __init__(self, target_pid):
        self.target_pid = target_pid
        self.fusion_debug_port = None
        self.timeout = 1
        super().__init__(("localhost", 0), SSDPResponseHandler)

    def handle_error(self, request, client_address):
        sys.stderr.write("An error occurred while processing SSDP request: {}" % traceback.format_exc())

    def search(self):
        message = ("M-SEARCH * HTTP/1.1\r\n" +
                   'MAN: "ssdp:discover"\r\n' +
                   "MX: 1\r\n" +
                   "ST: fusion_idea:debug\r\n" +
                   "HOST: 127.0.0.1:1900\r\n\r\n")

        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        self.socket.sendto(message.encode("utf-8"), ("127.255.255.255", 1900))


def main(setup):
    json_dict = {"pydevd_path": setup["pydevd_path"],
                 "script": setup["script"],
                 "detach": setup["script"] and setup["debug"],
                 "debug_port": setup["port"]}

    ssdp = SSDPClient(setup["pid"])

    ssdp.search()

    start_time = time.time()
    while not ssdp.fusion_debug_port:
        ssdp.handle_request()
        if time.time() - start_time > 1:
            break

    if not ssdp.fusion_debug_port:
        sys.stderr.write("Could not contact the Fusion 360 process. Is the fusion_idea_addin running?")
        sys.exit(1)

    json_string = json.dumps(json_dict).encode("utf8")
    request = urllib.request.Request(
        "http://localhost:%s" % ssdp.fusion_debug_port,
        data=json_string,
        headers={"content-type": "application/json"})
    urllib.request.urlopen(request)


if __name__ == "__main__":
    main(process_command_line(sys.argv[1:]))
