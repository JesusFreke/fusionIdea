package org.jf.fusionIdea.executor;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class FusionDebugExecutor extends DefaultDebugExecutor {

    public static final String ID = "Debug in Fusion 360";

    public FusionDebugExecutor() {
    }

    public static Executor getInstance() {
        return ExecutorRegistry.getInstance().getExecutorById(ID);
    }

    @NotNull @Override public String getActionName() {
        return "Debug in Fusion 360";
    }

    @Override public boolean isApplicable(@NotNull Project project) {
        return FusionRunExecutor.getInstance().isApplicable(project);
    }

    @NotNull @Override public String getStartActionText() {
        return getActionName();
    }

    @Override public String getStartActionText(String configurationName) {
        return "Debug in Fusion 360";
    }

    @NotNull @Override public String getId() {
        return ID;
    }

    @Override public String getContextActionId() {
        return "DebugFusionScript";
    }
}
