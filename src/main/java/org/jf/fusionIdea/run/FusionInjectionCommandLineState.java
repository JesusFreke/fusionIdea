package org.jf.fusionIdea.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.jetbrains.python.debugger.attach.PyAttachToProcessCommandLineState;
import com.jetbrains.python.run.PythonConfigurationType;
import com.jetbrains.python.run.PythonRunConfiguration;
import com.jetbrains.python.run.PythonScriptCommandLineState;
import com.jetbrains.python.sdk.PythonEnvUtil;
import org.jetbrains.annotations.NotNull;
import org.jf.fusionIdea.FusionIdeaPlugin;

import java.io.File;
import java.util.StringJoiner;

import static com.jetbrains.python.PythonHelpersLocator.getHelpersRoot;

public class FusionInjectionCommandLineState extends PythonScriptCommandLineState {

    private final boolean debug;

    public FusionInjectionCommandLineState(PythonRunConfiguration pythonRunConfiguration,
                                           FusionRunConfiguration fusionRunConfiguration, ExecutionEnvironment env,
                                           boolean debug) {
        super(pythonRunConfiguration, env);
        this.debug = debug;
        setMultiprocessDebug(false);
    }

    public static FusionInjectionCommandLineState create(
            @NotNull Project project, @NotNull String sdkPath, FusionRunConfiguration fusionConfiguration, int pid,
            boolean debug, int port)
            throws ExecutionException {
        PythonRunConfiguration pythonConfiguration = (PythonRunConfiguration) PythonConfigurationType
                .getInstance().getFactory().createTemplateConfiguration(project);

        IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId(FusionIdeaPlugin.ID));
        String injectScriptPath =
                new File(new File(plugin.getPath(), "scripts"), "inject.py").getAbsolutePath();

        pythonConfiguration.setScriptName(injectScriptPath);
        pythonConfiguration.setSdkHome(sdkPath);
        pythonConfiguration.setUseModuleSdk(false);
        PythonEnvUtil.addToPythonPath(pythonConfiguration.getEnvs(),
                new File(getHelpersRoot(), "pydev/pydevd_attach_to_process").getAbsolutePath());

        StringJoiner params = new StringJoiner(" ");

        if (fusionConfiguration != null) {
            params.add("--script");
            params.add(new File(fusionConfiguration.getScript()).getAbsolutePath());
        }

        params.add("--pid");
        params.add(String.valueOf(pid));
        params.add("--debug");
        params.add(debug ? "1" : "0");
        if (port > 0) {
            params.add("--port");
            params.add(String.valueOf(port));
        }

        pythonConfiguration.setScriptParameters(params.toString());

        Executor executor;
        if (debug) {
            executor = DefaultDebugExecutor.getDebugExecutorInstance();
        } else {
            executor = DefaultRunExecutor.getRunExecutorInstance();
        }
        ExecutionEnvironment env = ExecutionEnvironmentBuilder.create(project, executor, pythonConfiguration).build();

        return new FusionInjectionCommandLineState(pythonConfiguration, fusionConfiguration, env, debug);
    }

    @Override
    protected ProcessHandler doCreateProcess(GeneralCommandLine commandLine) throws ExecutionException {

        if (debug) {
            ProcessHandler handler = super.doCreateProcess(commandLine);

            return new PyAttachToProcessCommandLineState.PyRemoteDebugProcessHandler(handler) {
                @Override public boolean detachIsDefault() {
                    return true;
                }
            };
        } else {
            return super.doCreateProcess(commandLine);
        }
    }
}
