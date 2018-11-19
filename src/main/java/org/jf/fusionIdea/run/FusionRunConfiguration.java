package org.jf.fusionIdea.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.attach.LocalAttachHost;
import com.jetbrains.python.run.AbstractPythonRunConfiguration;
import com.jetbrains.python.run.DebugAwareConfiguration;
import com.jetbrains.python.sdk.PythonSdkType;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;


public class FusionRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule>
        implements DebugAwareConfiguration {

    private String script;

    public FusionRunConfiguration(Project project, ConfigurationFactory factory) {
        super(new RunConfigurationModule(project), factory);
        getConfigurationModule().init();

        final Module[] modules = ModuleManager.getInstance(project).getModules();
    }

    @Override public Collection<Module> getValidModules() {
        return AbstractPythonRunConfiguration.getValidModules(this.getProject());
    }

    @Override public boolean canRunUnderDebug() {
        return true;
    }

    @NotNull @Override public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new FusionRunConfigurationEditor(this);
    }

    @Nullable @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment)
            throws ExecutionException {
        // TODO: check if there are multiple Fusion 360 processes, and show a chooser dialog
        int pid = -1;
        for (ProcessInfo processInfo : LocalAttachHost.INSTANCE.getProcessList()) {
            if (StringUtil.containsIgnoreCase(processInfo.getExecutableName(), "Fusion360.exe")) {
                pid = processInfo.getPid();
            }
        }

        // TODO: double-check that we found a process, show an error dialog?
        return FusionInjectionCommandLineState.create(getProject(),
                PythonSdkType.findPythonSdk(getModule()).getHomePath(),
                this, pid, executor.getId() == DefaultDebugExecutor.EXECUTOR_ID, -1);
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
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
    }

    @Override public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        script = JDOMExternalizerUtil.readField(element, "script");
    }
}
