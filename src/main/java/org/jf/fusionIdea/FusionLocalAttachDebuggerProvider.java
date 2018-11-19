package org.jf.fusionIdea;

import com.google.common.collect.Lists;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.attach.XLocalAttachDebugger;
import com.jetbrains.python.debugger.PyDebugRunner;
import com.jetbrains.python.debugger.PyLocalPositionConverter;
import com.jetbrains.python.debugger.PyRemoteDebugProcess;
import com.jetbrains.python.run.AbstractPythonRunConfiguration;
import com.jetbrains.python.sdk.PreferredSdkComparator;
import com.jetbrains.python.sdk.PythonSdkType;
import org.jetbrains.annotations.NotNull;
import org.jf.fusionIdea.run.FusionInjectionCommandLineState;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FusionLocalAttachDebuggerProvider extends com.jetbrains.python.debugger.attach.PyLocalAttachDebuggerProvider {

    private static final int CONNECTION_TIMEOUT = 20000;

    @NotNull
    private static List<XLocalAttachDebugger> getAttachDebuggersForAllLocalSdks(@NotNull Project project) {
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
        final List<XLocalAttachDebugger> result = PythonSdkType.getAllLocalCPythons()
                .stream()
                .filter(sdk -> sdk != selectedSdk)
                .filter(sdk -> !PythonSdkType.isInvalid(sdk))
                .sorted(PreferredSdkComparator.INSTANCE)
                .map(PyLocalAttachDebugger::new)
                .collect(Collectors.toList());
        if (selectedSdk != null) {
            result.add(0, new PyLocalAttachDebugger(selectedSdk));
        }
        return result;
    }

    @NotNull @Override
    public List<XLocalAttachDebugger> getAvailableDebuggers(
            @NotNull Project project, @NotNull ProcessInfo processInfo, @NotNull UserDataHolder contextHolder) {

        if (StringUtil.containsIgnoreCase(processInfo.getCommandLine(), "Fusion360.exe")) {
            List<XLocalAttachDebugger> result = Collections.emptyList();

            if (processInfo.getExecutableCannonicalPath().isPresent() &&
                    new File(processInfo.getExecutableCannonicalPath().get()).exists()) {
                result = Lists.newArrayList(new PyLocalAttachDebugger(processInfo.getExecutableCannonicalPath().get()));
            } else {
                result = getAttachDebuggersForAllLocalSdks(project);
            }

            return result;
        }
        return Collections.emptyList();
    }

    private static class PyLocalAttachDebugger implements XLocalAttachDebugger {
        private final String sdkPath;
        @NotNull private final String myName;

        public PyLocalAttachDebugger(@NotNull Sdk sdk) {
            sdkPath = sdk.getHomePath();
            myName = PythonSdkType.getInstance().getVersionString(sdk) + " (" + sdkPath + ")";
        }

        public PyLocalAttachDebugger(@NotNull String sdkPath) {
            this.sdkPath = sdkPath;
            myName = "Python Debugger";
        }

        @NotNull
        @Override
        public String getDebuggerDisplayName() {
            return myName;
        }

        @Override
        public void attachDebugSession(@NotNull Project project, @NotNull ProcessInfo processInfo) throws ExecutionException {
            FusionLocalAttachDebugRunner runner =
                    new FusionLocalAttachDebugRunner(project, sdkPath, processInfo.getPid());
            runner.launch();
        }
    }

    private static class FusionLocalAttachDebugRunner extends PyDebugRunner {

        private final Project project;
        private final String sdkPath;
        private final int pid;

        public FusionLocalAttachDebugRunner(Project project, String sdkPath, int pid) {
            this.project = project;
            this.sdkPath = sdkPath;
            this.pid = pid;
        }

        public XDebugSession launch() throws ExecutionException {
            FileDocumentManager.getInstance().saveAllDocuments();

            return launchRemoteDebugServer();
        }

        private XDebugSession launchRemoteDebugServer() throws ExecutionException {
            final ServerSocket serverSocket;
            try {
                //noinspection SocketOpenedButNotSafelyClosed
                serverSocket = new ServerSocket(0);
            }
            catch (IOException e) {
                throw new ExecutionException("Failed to find free socket port", e);
            }

            FusionInjectionCommandLineState state = FusionInjectionCommandLineState.create(
                    project, sdkPath, null, pid, true, serverSocket.getLocalPort());

            final ExecutionResult result = state.execute(state.getEnvironment().getExecutor(), this);

            //start remote debug server
            return XDebuggerManager.getInstance(project).
                    startSessionAndShowTab(String.valueOf(pid), null, new XDebugProcessStarter() {
                        @org.jetbrains.annotations.NotNull
                        public XDebugProcess start(@NotNull final XDebugSession session) {
                            PyRemoteDebugProcess pyDebugProcess =
                                    new PyRemoteDebugProcess(session, serverSocket, result.getExecutionConsole(),
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
                                            return "Attaching to a process with PID=" + pid;
                                        }

                                        @Override
                                        protected String getConnectionTitle() {
                                            return "Attaching Debugger";
                                        }
                                    };
                            pyDebugProcess.setPositionConverter(new PyLocalPositionConverter());


                            createConsoleCommunicationAndSetupActions(project, result, pyDebugProcess, session);

                            return pyDebugProcess;
                        }
                    });
        }
    }
}
