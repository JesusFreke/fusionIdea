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
