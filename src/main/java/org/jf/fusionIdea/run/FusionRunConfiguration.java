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

import com.intellij.execution.*;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.python.run.AbstractPythonRunConfiguration;
import com.jetbrains.python.sdk.PythonSdkUtil;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.fusionIdea.executor.FusionDebugExecutor;

import java.io.File;
import java.util.Collection;
import java.util.List;


public class FusionRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule, Void>
        implements RunConfigurationWithSuppressedDefaultDebugAction, RunConfigurationWithSuppressedDefaultRunAction {

    private String script;
    @Nullable
    private String sdkName;
    private boolean useModuleSdk;

    public FusionRunConfiguration(Project project, ConfigurationFactory factory) {
        super(new RunConfigurationModule(project), factory);
        getConfigurationModule().setModuleToAnyFirstIfNotSpecified();
    }

    @Override public boolean canRunOn(@NotNull ExecutionTarget target) {
        return target instanceof FusionExecutionTarget;
    }

    @Override public Collection<Module> getValidModules() {
        return AbstractPythonRunConfiguration.getValidModules(this.getProject());
    }

    @NotNull @Override public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new FusionRunConfigurationEditor(this);
    }

    @Nullable @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment)
            throws ExecutionException {

        FusionExecutionTarget target = (FusionExecutionTarget) environment.getExecutionTarget();
        if (!target.isReady()) {
            ExecutionTargetManager manager = ExecutionTargetManager.getInstance(getProject());
            RunnerAndConfigurationSettings runnerAndConfigurationSettings = environment.getRunnerAndConfigurationSettings();
            RunConfiguration runConfiguration = null;
            if (runnerAndConfigurationSettings != null) {
                runConfiguration = runnerAndConfigurationSettings.getConfiguration();
            }

            List<ExecutionTarget> targets = manager.getTargetsFor(runConfiguration);
            if (targets.size() == 1) {
                // If there's only a single process, we'll just automatically update and use the new target.
                manager.update();
                target = (FusionExecutionTarget) manager.getActiveTarget();
            } else {
                // If there are multiple available processes, we should let the user update the active process, so we
                // don't run in the wrong one.
                throw new ExecutionException(
                        "The selected Fusion 360 Process is no longer valid. Please select a new process.");
            }
        }

        VirtualFile scriptFile = LocalFileSystem.getInstance().findFileByPath(getScript());
        if (scriptFile == null) {
            return null;
        }

        Sdk sdk = getSdk();
        if (sdk == null) {
            return null;
        }

        return new FusionScriptState(
                getProject(), this, target.getPid(), executor.getId().equals(FusionDebugExecutor.ID));
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public void setSdkName(@Nullable String sdkName) {
        this.sdkName = sdkName;
    }

    @Nullable
    public String getSdkName() {
        return sdkName;
    }

    @Nullable public Sdk getSdk() {
        if (useModuleSdk) {
            return PythonSdkUtil.findPythonSdk(getModule());
        } else if (StringUtil.isEmpty(getSdkName())) {
            return ProjectRootManager.getInstance(getProject()).getProjectSdk();
        }
        return PythonSdkUtil.findSdkByKey(getSdkName());
    }

    public boolean useModuleSdk() {
        return useModuleSdk;
    }

    public void setUseModuleSdk(boolean useModuleSdk) {
        this.useModuleSdk = useModuleSdk;
    }

    @NotNull
    public String getWorkingDirectory() {
        final String result = getProject().getBasePath();
        if (result != null) {
            return result;
        }

        final String firstModuleRoot = getFirstModuleRoot();
        if (firstModuleRoot != null) {
            return firstModuleRoot;
        }
        return new File(".").getAbsolutePath();
    }

    @Nullable
    private String getFirstModuleRoot() {
        final Module module = getModule();
        if (module == null) {
            return null;
        }
        final VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
        return roots.length > 0 ? roots[0].getPath() : null;
    }

    @Nullable
    public Module getModule() {
        return getConfigurationModule().getModule();
    }

    @Override public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);
        JDOMExternalizerUtil.writeField(element, "script", script);
        JDOMExternalizerUtil.writeField(element, "sdkName", sdkName);
        JDOMExternalizerUtil.writeField(element, "useModuleSdk", Boolean.toString(useModuleSdk));
    }

    @Override public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        script = JDOMExternalizerUtil.readField(element, "script");
        sdkName = JDOMExternalizerUtil.readField(element, "sdkName");
        if (StringUtils.isEmpty(sdkName)) {
            // sdkPath was replaced with sdkName, but this is to migrate old projects that still have sdkPath set
            String sdkHome = JDOMExternalizerUtil.readField(element, "sdkHome");
            if (!StringUtils.isEmpty(sdkHome)) {
                Sdk sdk = PythonSdkUtil.findSdkByPath(sdkHome);
                if (sdk != null) {
                    sdkName = sdk.getName();
                }
            }
        }
        useModuleSdk = Boolean.parseBoolean(JDOMExternalizerUtil.readField(element, "useModuleSdk"));
    }

    @Nullable @Override public String suggestedName() {
        if (script == null) {
            return null;
        } else {
            String name = (new File(script)).getName();
            return name.endsWith(".py") ? name.substring(0, name.length() - 3) : name;
        }
    }
}
