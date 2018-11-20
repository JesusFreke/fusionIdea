package org.jf.fusionIdea.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.Executor;
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
import com.jetbrains.python.sdk.PythonSdkType;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.fusionIdea.executor.FusionDebugExecutor;

import java.io.File;
import java.util.Collection;


public class FusionRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule>
        implements RunConfigurationWithSuppressedDefaultDebugAction, RunConfigurationWithSuppressedDefaultRunAction {

    private String script;
    private String sdkHome;
    private boolean useModuleSdk;

    public FusionRunConfiguration(Project project, ConfigurationFactory factory) {
        super(new RunConfigurationModule(project), factory);
        getConfigurationModule().init();
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

        VirtualFile scriptFile = LocalFileSystem.getInstance().findFileByPath(getScript());
        if (scriptFile == null) {
            return null;
        }

        FusionRunConfiguration configuration =
                (FusionRunConfiguration) environment.getRunnerAndConfigurationSettings().getConfiguration();

        Sdk sdk = configuration.getSdk();
        if (sdk == null) {
            return null;
        }

        return FusionInjectionCommandLineState.create(getProject(), sdk.getHomePath(), this, target.getPid(),
                executor.getId().equals(FusionDebugExecutor.ID), -1);
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getSdkHome() {
        return sdkHome;
    }

    public void setSdkHome(String sdkHome) {
        this.sdkHome = sdkHome;
    }

    @Nullable public Sdk getSdk() {
        if (useModuleSdk) {
            return PythonSdkType.findPythonSdk(getModule());
        } else if (StringUtil.isEmpty(getSdkHome())) {
            return ProjectRootManager.getInstance(getProject()).getProjectSdk();
        }
        return PythonSdkType.findSdkByPath(getSdkHome());
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
        JDOMExternalizerUtil.writeField(element, "sdkHome", sdkHome);
        JDOMExternalizerUtil.writeField(element, "useModuleSdk", Boolean.toString(useModuleSdk));
    }

    @Override public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        script = JDOMExternalizerUtil.readField(element, "script");
        sdkHome = JDOMExternalizerUtil.readField(element, "sdkHome");
        useModuleSdk = Boolean.parseBoolean(JDOMExternalizerUtil.readField(element, "useModuleSdk"));
    }
}
