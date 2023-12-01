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

package org.jf.fusionIdea.attach;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.attach.XAttachDebugger;
import com.intellij.xdebugger.attach.XAttachHost;
import com.intellij.xdebugger.attach.XAttachPresentationGroup;
import com.jetbrains.python.debugger.PyDebugRunner;
import com.jetbrains.python.debugger.PyLocalPositionConverter;
import com.jetbrains.python.debugger.PyRemoteDebugProcess;
import com.jetbrains.python.debugger.attach.PyLocalAttachDebuggerProvider;
import com.jetbrains.python.run.AbstractPythonRunConfiguration;
import com.jetbrains.python.sdk.PreferredSdkComparator;
import com.jetbrains.python.sdk.PythonSdkType;
import com.jetbrains.python.sdk.PythonSdkUtil;
import org.jetbrains.annotations.NotNull;
import org.jf.fusionIdea.facet.FusionFacet;
import org.jf.fusionIdea.run.FusionScriptState;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FusionLocalAttachDebuggerProvider extends PyLocalAttachDebuggerProvider {

    private static final Key<Set<String>> FUSION_EXECUTABLES =
            Key.create("FusionLocalAttachDebuggerProvider.FUSION_EXECUTABLES");

    private static final int CONNECTION_TIMEOUT = 20000;

    @NotNull
    private static List<XAttachDebugger> getAttachDebuggersForAllLocalSdks(@NotNull Project project) {
        Sdk selected = null;
        RunnerAndConfigurationSettings settings = RunManager.getInstance(project).getSelectedConfiguration();
        if (settings != null) {
            RunConfiguration runConfiguration = settings.getConfiguration();
            if (runConfiguration instanceof AbstractPythonRunConfiguration) {
                selected = ((AbstractPythonRunConfiguration)runConfiguration).getSdk();
            }
        }

        final Sdk selectedSdk = selected;
        // most recent python version goes first
        final List<XAttachDebugger> result = PythonSdkUtil.getAllLocalCPythons()
                .stream()
                .filter(sdk -> sdk != selectedSdk)
                // TODO: deprecated usage, replace with PySdkExtKt.sdkSeemsValid(sdk), once available per our minimum IDEA version
                .filter(sdk -> !PythonSdkUtil.isInvalid(sdk))
                .sorted(PreferredSdkComparator.INSTANCE)
                .map(PyLocalAttachDebugger::new)
                .collect(Collectors.toList());
        if (selectedSdk != null) {
            result.add(0, new PyLocalAttachDebugger(selectedSdk));
        }
        return result;
    }

    @NotNull @Override
    public List<XAttachDebugger> getAvailableDebuggers(
            @NotNull Project project, @NotNull XAttachHost hostInfo, @NotNull ProcessInfo processInfo, @NotNull UserDataHolder contextHolder) {

        Set<String> fusionExecutables = contextHolder.getUserData(FUSION_EXECUTABLES);
        if (fusionExecutables == null) {
            fusionExecutables = new HashSet();
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                FusionFacet facet = FusionFacet.getInstance(module);
                if (facet != null) {
                    String fusionPath = facet.getConfiguration().getFusionPath();
                    if (fusionPath != null) {
                        try {
                            fusionExecutables.add(new File(facet.getConfiguration().getFusionPath()).getCanonicalPath());
                        } catch (IOException e) {
                        }
                    }
                }
            }
            contextHolder.putUserData(FUSION_EXECUTABLES, fusionExecutables);
        }

        for (String fusionExecutable : fusionExecutables) {
            if (processInfo.getCommandLine().contains(fusionExecutable)) {
                return getAttachDebuggersForAllLocalSdks(project);
            }
        }

        return Collections.emptyList();
    }

    @NotNull @Override public XAttachPresentationGroup<ProcessInfo> getPresentationGroup() {
        return FusionLocalAttachGroup.INSTANCE;
    }

    private static class PyLocalAttachDebugger implements XAttachDebugger {
        private final String sdkPath;
        @NotNull private final String myName;

        public PyLocalAttachDebugger(@NotNull Sdk sdk) {
            sdkPath = sdk.getHomePath();
            myName = PythonSdkType.getInstance().getVersionString(sdk) + " (" + sdkPath + ")";
        }

        @NotNull
        @Override
        public String getDebuggerDisplayName() {
            return myName;
        }

        @Override
        public void attachDebugSession(@NotNull Project project, @NotNull XAttachHost hostInfo, @NotNull ProcessInfo processInfo) throws ExecutionException {
            launchDebugger(project, processInfo.getPid());
        }
    }

    private static void launchDebugger(Project project, int pid) throws ExecutionException {
        FileDocumentManager.getInstance().saveAllDocuments();


        FusionScriptState state = new FusionScriptState(project, null, pid, true);
        state.execute(0).then(result -> {
            try {
                //start remote debug server
                return XDebuggerManager.getInstance(project).
                        startSessionAndShowTab(String.valueOf(pid), null, new XDebugProcessStarter() {
                            @NotNull
                            public XDebugProcess start(@NotNull final XDebugSession session) throws ExecutionException {
                                int port = state.getServerSocket().getLocalPort();
                                PyRemoteDebugProcess pyDebugProcess =
                                        new PyRemoteDebugProcess(
                                                session, state.getServerSocket(), result.getExecutionConsole(),
                                                result.getProcessHandler(), "") {
                                            @Override
                                            protected void printConsoleInfo() {
                                            }

                                            @Override
                                            public int getConnectTimeout() {
                                                return CONNECTION_TIMEOUT;
                                            }

                                            @Override
                                            protected void detachDebuggedProcess() {
                                                handleStop();
                                            }

                                            @Override
                                            protected String getConnectionMessage() {
                                                return "Attaching to a process with PID=" + pid + " and port=" + port;
                                            }

                                            @Override
                                            protected String getConnectionTitle() {
                                                return "Attaching Debugger";
                                            }
                                        };
                                pyDebugProcess.setPositionConverter(new PyLocalPositionConverter());

                                PyDebugRunner.createConsoleCommunicationAndSetupActions(project, result, pyDebugProcess, session);

                                return pyDebugProcess;
                            }
                        });
            } catch (ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
