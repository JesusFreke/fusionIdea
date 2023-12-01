/*
 * Copyright 2023, Ben Gruver
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

package org.jf.fusionIdea.inspection;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PycharmFusionExecutableInspection extends FusionExecutableInspection {
    @Override protected List<LocalQuickFix> getQuickFixesForUpdatedFusionExecutable(@Nullable String newPath) {
        List<LocalQuickFix> fixes = new ArrayList<>();
        if (newPath != null) {
            fixes.add(new UpdateFusionExecutableInAllModules(newPath));
        }
        fixes.add(new OpenFacetConfiguration());
        return fixes;
    }

    public class UpdateFusionExecutableInAllModules extends FusionExecutableInspection.UpdateFusionExecutableInAllModules {

        public UpdateFusionExecutableInAllModules(String newPath) {
            super(newPath);
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return "Update Fusion 360 executable";
        }
    }

    public class OpenFacetConfiguration extends FusionExecutableInspection.OpenFacetConfiguration {

        @NotNull
        @Override
        public String getFamilyName() {
            return "Open Fusion 360 plugin configuration";
        }

        @Override
        public void applyFix(@NotNull final Project project, @NotNull ProblemDescriptor descriptor) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "Fusion 360");
        }
    }
}
