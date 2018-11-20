package org.jf.fusionIdea.run;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.ParamsGroup;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.project.Project;
import com.jetbrains.python.debugger.PyDebugRunner;
import com.jetbrains.python.run.CommandLinePatcher;
import com.jetbrains.python.run.PythonCommandLineState;
import org.jetbrains.annotations.NotNull;
import org.jf.fusionIdea.executor.FusionDebugExecutor;
import org.jf.fusionIdea.executor.FusionRunExecutor;

public class FusionDebugRunner extends PyDebugRunner {

    @Override public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        if (!executorId.equals(FusionRunExecutor.ID) && !executorId.equals(FusionDebugExecutor.ID)) {
            return false;
        }
        if (profile instanceof FusionRunConfiguration) {
            return true;
        }
        return false;
    }

    @NotNull @Override public String getRunnerId() {
        return "FusionDebugRunner";
    }

    @Override
    public CommandLinePatcher[] createCommandLinePatchers(
            Project project, PythonCommandLineState state, RunProfile profile, int serverLocalPort) {

        return new CommandLinePatcher[] {
                new CommandLinePatcher() {
                    @Override public void patchCommandLine(GeneralCommandLine generalCommandLine) {
                        int groups = generalCommandLine.getParametersList().getParamsGroupsCount();

                        ParamsGroup group = generalCommandLine.getParametersList().getParamsGroupAt(groups-1);

                        group.addParameters("--port", String.valueOf(serverLocalPort));
                    }
                }
        };
    }
}
