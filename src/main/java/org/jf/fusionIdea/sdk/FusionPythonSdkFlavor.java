/*
 * Copyright 2019, Ben Gruver
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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.SystemInfo;
import com.jetbrains.python.sdk.flavors.PythonFlavorProvider;
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.fusionIdea.FusionIdeaIcons;
import org.jf.fusionIdea.facet.FusionFacet;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FusionPythonSdkFlavor extends PythonSdkFlavor {
    public static final FusionPythonSdkFlavor INSTANCE = new FusionPythonSdkFlavor();

    @NotNull @Override public String getName() {
        return "Fusion 360 Python SDK";
    }

    @Override public Collection<String> suggestHomePaths(@Nullable Module module) {
        List<String> paths = new ArrayList<>();

        String sdkPath = findFusionPythonSdkPath();
        if (sdkPath != null) {
            paths.add(sdkPath);
        }

        return paths;
    }

    @Nullable
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

    @Nullable
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

    @Nullable
    private String findFusionPythonSdkPath() {
        if (SystemInfo.isWindows) {
            return findFusionPythonSdkPathWindows();
        } else if (SystemInfo.isMac) {
            return findFusionPythonSdkPathMac();
        }
        return null;
    }

    @Override public Icon getIcon() {
        return FusionIdeaIcons.LOGO;
    }

    public static class Provider implements PythonFlavorProvider {
        @Nullable @Override public PythonSdkFlavor getFlavor(boolean b) {
            return INSTANCE;
        }
    }
}
