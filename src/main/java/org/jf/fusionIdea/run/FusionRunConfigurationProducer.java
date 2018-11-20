package org.jf.fusionIdea.run;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import com.jetbrains.python.PythonFileType;
import com.jetbrains.python.psi.types.TypeEvalContext;
import com.jetbrains.python.run.RunnableScriptFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FusionRunConfigurationProducer extends RunConfigurationProducer<FusionRunConfiguration> {

    public FusionRunConfigurationProducer() {
        super(FusionRunConfigurationType.getInstance());
    }

    @Override
    public boolean shouldReplace(@NotNull ConfigurationFromContext self, @NotNull ConfigurationFromContext other) {
        return false;
    }

    @Override
    protected boolean setupConfigurationFromContext(FusionRunConfiguration configuration,
                                                    ConfigurationContext context,
                                                    Ref<PsiElement> sourceElement) {
        Location location = context.getLocation();
        if (location == null) {
            return false;
        }
        PsiFile script = location.getPsiElement().getContainingFile();
        if (!isAvailable(location, script)) {
            return false;
        }

        VirtualFile vFile = script.getVirtualFile();
        if (vFile == null) {
            return false;
        }
        configuration.setScript(vFile.getPath());
        configuration.setName(configuration.suggestedName());
        return true;
    }

    @Override
    public boolean isConfigurationFromContext(FusionRunConfiguration configuration, ConfigurationContext context) {
        Location location = context.getLocation();
        if (location == null) {
            return false;
        }
        PsiFile script = location.getPsiElement().getContainingFile();
        if (!isAvailable(location, script)) {
            return false;
        }
        VirtualFile virtualFile = script.getVirtualFile();
        if (virtualFile == null) {
            return false;
        }
        if (virtualFile instanceof LightVirtualFile) {
            return false;
        }

        return configuration.getScript().equals(virtualFile.getPath());
    }

    private static boolean isAvailable(@NotNull final Location location, @Nullable final PsiFile script) {
        if (script == null || script.getFileType() != PythonFileType.INSTANCE) {
            return false;
        }
        final Module module = ModuleUtilCore.findModuleForPsiElement(script);
        if (module != null) {
            for (RunnableScriptFilter f : Extensions.getExtensions(RunnableScriptFilter.EP_NAME)) {
                // Configuration producers always called by user
                if (f.isRunnableScript(script, module, location, TypeEvalContext.userInitiated(location.getProject(), null))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isPreferredConfiguration(ConfigurationFromContext self, ConfigurationFromContext other) {
        return true;
    }
}
