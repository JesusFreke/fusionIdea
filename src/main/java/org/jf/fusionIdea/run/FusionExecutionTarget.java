package org.jf.fusionIdea.run;

import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.process.ProcessInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.fusionIdea.FusionIdeaIcons;

import javax.swing.*;

public class FusionExecutionTarget extends ExecutionTarget {
    private final ProcessInfo targetProcess;

    public FusionExecutionTarget(ProcessInfo targetProcess) {
        this.targetProcess = targetProcess;
    }

    @NotNull @Override public String getId() {
        return "FusionProcess" + targetProcess.getPid();
    }

    @NotNull @Override public String getDisplayName() {
        return "Fusion Process (" + targetProcess.getPid() + ")";
    }

    @Nullable @Override public Icon getIcon() {
        return FusionIdeaIcons.LOGO;
    }

    @Override public boolean canRun(@NotNull RunnerAndConfigurationSettings configuration) {
        return configuration.getType() == FusionRunConfigurationType.getInstance();
    }

    public int getPid() {
        return targetProcess.getPid();
    }
}
