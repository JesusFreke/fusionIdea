package org.jf.fusionIdea.run;

import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.ExecutionTargetProvider;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jf.fusionIdea.facet.FusionFacet;

import java.util.*;

public class FusionExecutionTargetProvider extends ExecutionTargetProvider {

    @NotNull @Override
    public List<ExecutionTarget> getTargets(@NotNull Project project,
                                            @NotNull RunnerAndConfigurationSettings configuration) {
        if (configuration.getType() != FusionRunConfigurationType.getInstance()) {
            return Collections.emptyList();
        }

        FusionRunConfiguration runConfiguration = (FusionRunConfiguration)configuration.getConfiguration();

        Map<Integer, ProcessInfo> targetProcesses = new HashMap<>();
        for (Module module : getApplicableModules(runConfiguration)) {
            FusionFacet facet = FusionFacet.getInstance(module);
            if (facet != null) {
                for (ProcessInfo processInfo : facet.findTargetProcesses()) {
                    targetProcesses.put(processInfo.getPid(), processInfo);
                }
            }
        }

        return buildTargets(targetProcesses.values());
    }

    private List<Module> getApplicableModules(FusionRunConfiguration runConfiguration) {
        Module module = runConfiguration.getModule();
        if (module != null) {
            return Collections.singletonList(module);
        } else {
            return Arrays.asList(ModuleManager.getInstance(runConfiguration.getProject()).getModules());
        }
    }

    private List<ExecutionTarget> buildTargets(Collection<ProcessInfo> targetProcesses) {
        List<ExecutionTarget> targets = new ArrayList<>();

        for (ProcessInfo targetProcess : targetProcesses) {
            targets.add(new FusionExecutionTarget(targetProcess));
        }

        return targets;
    }
}
