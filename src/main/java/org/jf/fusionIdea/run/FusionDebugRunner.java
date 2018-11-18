package org.jf.fusionIdea.run;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.ParamsGroup;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.jetbrains.python.debugger.PyDebugRunner;
import com.jetbrains.python.run.CommandLinePatcher;
import com.jetbrains.python.run.PythonCommandLineState;
import org.jetbrains.annotations.NotNull;

public class FusionDebugRunner extends PyDebugRunner {

    private static final Logger log = Logger.getInstance("FusionDebugRunner");

    @Override public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        if (!super.canRun(executorId, profile)) {
            return false;
        }
        if (profile instanceof FusionRunConfiguration) {
            return true;
        }
        return false;
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
