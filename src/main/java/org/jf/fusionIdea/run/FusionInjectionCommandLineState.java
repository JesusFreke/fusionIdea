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

import com.intellij.execution.CommandLineUtil;
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
import com.intellij.util.execution.ParametersListUtil;
import com.jetbrains.python.debugger.attach.PyAttachToProcessCommandLineState;
import com.jetbrains.python.run.PythonConfigurationType;
import com.jetbrains.python.run.PythonRunConfiguration;
import com.jetbrains.python.run.PythonScriptCommandLineState;
import com.jetbrains.python.sdk.PythonEnvUtil;
import org.jetbrains.annotations.NotNull;
import org.jf.fusionIdea.FusionIdeaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

        List<String> params = new ArrayList<>();

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

        pythonConfiguration.setScriptParameters(ParametersListUtil.join(params));

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
