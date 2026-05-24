/*
 * Copyright 2026, Ben Gruver
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

package org.jf.fusionIdea.sdk;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.platform.eel.EelApi;
import com.intellij.python.community.services.systemPython.SystemPythonProvider;
import com.jetbrains.python.PyToolUIInfo;
import com.jetbrains.python.Result;
import com.jetbrains.python.errorProcessing.PyError;
import kotlin.coroutines.Continuation;
import org.jf.fusionIdea.FusionIdeaIcons;
import org.jf.fusionIdea.facet.FusionFacet;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FusionSystemPythonProvider implements SystemPythonProvider {

    private static PyToolUIInfo FUSION_TOOL_UI_INFO = new PyToolUIInfo("Fusion 360 Python SDK", FusionIdeaIcons.LOGO);

    @Override
    public @Nullable Result<Set<Path>, PyError> findSystemPythons(@NonNull EelApi eelApi, @NonNull Continuation<? super Result<? extends Set<? extends Path>, ? extends PyError>> continuation) {
        List<Path> paths = new ArrayList<>();

        String sdkPath = findFusionPythonSdkPath();
        if (sdkPath != null) {
            paths.add(Path.of(sdkPath));
        }

        return Result.Companion.success(new HashSet<>(paths));
    }

    @Override public @Nullable PyToolUIInfo getUiCustomization() {
        return FUSION_TOOL_UI_INFO;
    }

    @org.jetbrains.annotations.Nullable
    private String findFusionPythonSdkPathWindows() {
        String fusionExePath = FusionFacet.autoDetectFusionPath();
        if (fusionExePath != null) {
            File fusionPythonSdkPath =
                    new File(new File(new File(fusionExePath).getParentFile(), "Python"), "python.exe");
            if (fusionPythonSdkPath.exists()) {
                return fusionPythonSdkPath.getAbsolutePath();
            }
        }
        return null;
    }

    @org.jetbrains.annotations.Nullable
    private String findFusionPythonSdkPathMac() {
        String fusionExePath = FusionFacet.autoDetectFusionPath();
        if (fusionExePath != null) {
            File fusionPythonSdkPath =
                    new File(new File(fusionExePath).getParentFile().getParentFile(),
                            "Frameworks/Python.framework/Versions/Current/bin/python");
            if (fusionPythonSdkPath.exists()) {

                return fusionPythonSdkPath.getAbsolutePath();
            }
        }
        return null;
    }

    @org.jetbrains.annotations.Nullable
    private String findFusionPythonSdkPath() {
        if (SystemInfo.isWindows) {
            return findFusionPythonSdkPathWindows();
        } else if (SystemInfo.isMac) {
            return findFusionPythonSdkPathMac();
        }
        return null;
    }
}
