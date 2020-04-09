/*
 * Copyright 2018, Ben Gruver
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.fusionIdea.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.AsyncProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.jetbrains.python.debugger.PyDebugProcess;
import com.jetbrains.python.debugger.PyDebugRunner;
import com.jetbrains.python.debugger.PyRemoteDebugProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;
import org.jf.fusionIdea.executor.FusionDebugExecutor;

import java.net.ServerSocket;

public class FusionDebugRunner extends AsyncProgramRunner<RunnerSettings> {

    @Override public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return executorId.equals(FusionDebugExecutor.ID) && profile instanceof FusionRunConfiguration;
    }

    @NotNull @Override public String getRunnerId() {
        return "FusionDebugRunner";
    }

    @NotNull @Override
    protected Promise<RunContentDescriptor> execute(
            @NotNull ExecutionEnvironment environment, @NotNull RunProfileState state) throws ExecutionException {
        return createSession(state, environment).then(XDebugSession::getRunContentDescriptor);
    }

    protected Promise<XDebugSession> createSession(@NotNull RunProfileState state,
                                                   @NotNull final ExecutionEnvironment environment)
            throws ExecutionException {
        FileDocumentManager.getInstance().saveAllDocuments();

        final FusionScriptState fusionScriptState = (FusionScriptState)state;

        final ServerSocket serverSocket = fusionScriptState.getServerSocket();

        return fusionScriptState.execute(serverSocket.getLocalPort()).then(result -> {
            try {
                return XDebuggerManager.getInstance(environment.getProject())
                        .startSession(environment, new XDebugProcessStarter() {
                            @Override
                            @NotNull
                            public XDebugProcess start(@NotNull final XDebugSession session) {
                                PyDebugProcess pyDebugProcess =
                                        new PyRemoteDebugProcess(
                                                session,
                                                serverSocket,
                                                result.getExecutionConsole(),
                                                result.getProcessHandler(), "") {
                                            protected void printConsoleInfo() {
                                            }

                                            public int getConnectTimeout() {
                                                return 20000;
                                            }

                                            protected void detachDebuggedProcess() {
                                                this.handleStop();
                                            }

                                            protected String getConnectionMessage() {
                                                return "Attaching to Fusion 360 process with PID=" +
                                                        fusionScriptState.getPid();
                                            }

                                            protected String getConnectionTitle() {
                                                return "Attaching Debugger";
                                            }
                                        };

                                PyDebugRunner.createConsoleCommunicationAndSetupActions(
                                        environment.getProject(), result, pyDebugProcess, session);
                                return pyDebugProcess;
                            }
                        });
            } catch (ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
