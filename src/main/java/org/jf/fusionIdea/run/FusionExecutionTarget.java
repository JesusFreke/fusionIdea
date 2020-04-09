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
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.process.ProcessInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.fusionIdea.FusionIdeaIcons;
import org.jf.fusionIdea.facet.FusionFacet;

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

    @Override public boolean isReady() {
        for (ProcessInfo processInfo : FusionFacet.getProcesses()) {
            if (processInfo.getPid() == this.targetProcess.getPid()) {
                return true;
            }
        }
        return false;
    }

    @Override public boolean canRun(@NotNull RunConfiguration configuration) {
        return configuration.getType() == FusionRunConfigurationType.getInstance();
    }
}
