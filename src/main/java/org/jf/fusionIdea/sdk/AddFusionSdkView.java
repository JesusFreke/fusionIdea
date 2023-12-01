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

import com.google.common.collect.Maps;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.FormBuilder;
import com.jetbrains.python.sdk.PyDetectedSdk;
import com.jetbrains.python.sdk.PythonSdkAdditionalData;
import com.jetbrains.python.sdk.PythonSdkType;
import com.jetbrains.python.sdk.add.PyAddSdkDialogFlowAction;
import com.jetbrains.python.sdk.add.PyAddSdkStateListener;
import com.jetbrains.python.sdk.add.PyAddSdkView;
import com.jetbrains.python.sdk.add.PySdkPathChoosingComboBox;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.fusionIdea.FusionIdeaIcons;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class AddFusionSdkView extends JPanel implements PyAddSdkView {

    private final PySdkPathChoosingComboBox sdkComboBox;
    private final List<? extends Sdk> existingSdks;

    public AddFusionSdkView(List<? extends Sdk> existingSdks) {
        this.existingSdks = existingSdks;

        BorderLayout borderLayout = new BorderLayout();
        setLayout(borderLayout);

        List<Sdk> sdks = new ArrayList<>();

        // TODO: deprecated usage, replace with suggestLocalHomePaths, once available per our minimum IDEA version
        for (String path : FusionPythonSdkFlavor.INSTANCE.suggestHomePaths(null, null)) {
            sdks.add(new PyDetectedSdk(path));
        }

        sdkComboBox = new PySdkPathChoosingComboBox(sdks, null);

        add(FormBuilder.createFormBuilder()
                .addLabeledComponent("Interpreter:", sdkComboBox)
                .getPanel());
    }

    @NotNull @Override public String getPanelName() {
        return "Fusion 360 Python SDK";
    }

    @NotNull @Override public Map<PyAddSdkDialogFlowAction, Boolean> getActions() {
        Map<PyAddSdkDialogFlowAction, Boolean> actionMap = Maps.newHashMap();
        Pair<PyAddSdkDialogFlowAction, Boolean> ok = PyAddSdkDialogFlowAction.OK.enabled();
        actionMap.put(ok.getFirst(), ok.getSecond());
        return actionMap;
    }

    @NotNull @Override public Component getComponent() {
        return this;
    }

    @NotNull @Override public Icon getIcon() {
        return FusionIdeaIcons.LOGO;
    }

    @Override public void addStateListener(@NotNull PyAddSdkStateListener pyAddSdkStateListener) {

    }

    @Override public void complete() {

    }

    @Nullable @Override public Sdk getOrCreateSdk() {
        PyDetectedSdk sdk = (PyDetectedSdk) sdkComboBox.getSelectedSdk();
        if (sdk == null) {
            return null;
        }

        VirtualFile sdkHomeDir = sdk.getHomeDirectory();
        if (sdkHomeDir != null) {
            return SdkConfigurationUtil.setupSdk(existingSdks.toArray(new Sdk[0]),
                    sdkHomeDir,
                    PythonSdkType.getInstance(),
                    false,
                    // TODO: deprecated usage, replace with PythonSdkAdditionalData constructor that takes a PyFlavorAndData instance, once available per our minimum IDEA version
                    new PythonSdkAdditionalData(FusionPythonSdkFlavor.INSTANCE),
                    "Fusion 360 Python SDK");
        }
        return sdk;
    }

    @Override public void next() {
        throw new UnsupportedOperationException();
    }

    @Override public void onSelected() {
    }

    @Override public void previous() {
        throw new UnsupportedOperationException();
    }

    @NotNull @Override public List<ValidationInfo> validateAll() {
        return new ArrayList<>();
    }
}
