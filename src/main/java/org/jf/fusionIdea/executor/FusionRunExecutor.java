package org.jf.fusionIdea.executor;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jf.fusionIdea.facet.FusionFacet;

public class FusionRunExecutor extends DefaultRunExecutor {

    public static final String ID = "Run in Fusion 360";

    public FusionRunExecutor() {
    }

    public static Executor getInstance() {
        return ExecutorRegistry.getInstance().getExecutorById(ID);
    }

    @NotNull @Override public String getActionName() {
        return "Run in Fusion 360";
    }

    @Override public boolean isApplicable(@NotNull Project project) {
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            if (FusionFacet.getInstance(module) != null) {
                return true;
            }
        }
        return false;
    }

    @NotNull @Override public String getStartActionText() {
        return getActionName();
    }

    @Override public String getStartActionText(String configurationName) {
        return "Run in Fusion 360";
    }

    @NotNull @Override public String getId() {
        return ID;
    }

    @Override public String getContextActionId() {
        return "RunFusionScript";
    }
}
