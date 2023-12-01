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

import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.SdkListCellRenderer;
import com.intellij.openapi.roots.ui.configuration.ModulesAlphaComparator;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.PanelWithAnchor;
import com.jetbrains.extensions.python.FileChooserDescriptorExtKt;
import com.jetbrains.python.run.AbstractPythonRunConfiguration;
import com.jetbrains.python.sdk.PreferredSdkComparator;
import com.jetbrains.python.sdk.PythonSdkUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class FusionRunConfigurationEditor extends SettingsEditor<FusionRunConfiguration> implements PanelWithAnchor {

    private JPanel panel;
    private JComponent anchor;
    private TextFieldWithBrowseButton scriptTextField;
    private JRadioButton myUseModuleSdkRadioButton;
    private ModulesComboBox myModuleComboBox;
    private JRadioButton myUseSpecifiedSdkRadioButton;
    private ComboBox<Sdk> myInterpreterComboBox;

    public FusionRunConfigurationEditor(FusionRunConfiguration configuration) {
        FileChooserDescriptor chooserDescriptor = FileChooserDescriptorExtKt.withPythonFiles(
                FileChooserDescriptorFactory.createSingleFileDescriptor().withTitle("Select Script"), true);

        TextBrowseFolderListener listener =
                new TextBrowseFolderListener(chooserDescriptor, configuration.getProject()) {
                    @Nullable @Override protected VirtualFile getInitialFile() {
                        VirtualFile initialFile = super.getInitialFile();
                        if (initialFile != null) {
                            return initialFile;
                        }

                        return LocalFileSystem.getInstance().findFileByPath(configuration.getWorkingDirectory());
                    }
                };
        scriptTextField.addBrowseFolderListener(listener);

        List<Module> validPythonModules = AbstractPythonRunConfiguration.getValidModules(configuration.getProject());
        validPythonModules.sort(new ModulesAlphaComparator());
        Module selection = !validPythonModules.isEmpty() ? validPythonModules.get(0) : null;

        myModuleComboBox.setModules(validPythonModules);
        myModuleComboBox.setSelectedModule(selection);

        myInterpreterComboBox.setMinimumAndPreferredWidth(100);
        myInterpreterComboBox.setRenderer(new SdkListCellRenderer("<Project Default>"));

        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateControls();
            }
        };
        myUseSpecifiedSdkRadioButton.addActionListener(actionListener);
        myUseModuleSdkRadioButton.addActionListener(actionListener);
        myInterpreterComboBox.addActionListener(actionListener);
        myModuleComboBox.addActionListener(actionListener);

        setSdkName(null);
    }

    protected void resetEditorFrom(@NotNull FusionRunConfiguration configuration) {
        setScript(configuration.getScript());
        setSdkName(configuration.getSdkName());
        setUseModuleSdk(configuration.useModuleSdk());

        if (configuration.useModuleSdk()) {
            setModule(configuration.getModule());
        }

        updateControls();
    }

    protected void applyEditorTo(@NotNull FusionRunConfiguration configuration) throws ConfigurationException {
        configuration.setScript(getScript());
        configuration.setSdkName(getSdkName());

        configuration.setUseModuleSdk(isUseModuleSdk());

        if (isUseModuleSdk()) {
            configuration.setModule(getModule());
        }
    }

    @NotNull
    protected JComponent createEditor() {
        return panel;
    }

    @Override
    public JComponent getAnchor() {
        return anchor;
    }

    @Override
    public void setAnchor(JComponent anchor) {
        this.anchor = anchor;
    }

    public void setScript(String script) {
        scriptTextField.setText(script);
    }

    @Nullable
    public String getScript() {
        return scriptTextField.getText();
    }

    private void updateControls() {
        myModuleComboBox.setEnabled(myUseModuleSdkRadioButton.isSelected());
        myInterpreterComboBox.setEnabled(myUseSpecifiedSdkRadioButton.isSelected());
    }

    @Nullable
    public String getSdkName() {
        Sdk selectedSdk = (Sdk)myInterpreterComboBox.getSelectedItem();
        return selectedSdk == null ? null : selectedSdk.getName();
    }

    public void setSdkName(@Nullable String sdkName) {
        List<Sdk> sdkList = new ArrayList<>();
        sdkList.add(null);
        final List<Sdk> allSdks = new ArrayList<>(PythonSdkUtil.getAllSdks());
        allSdks.sort(new PreferredSdkComparator());
        Sdk selection = null;
        for (Sdk sdk : allSdks) {
            if (sdkName != null && sdk.getName().equals(sdkName)) {
                selection = sdk;
            }
            sdkList.add(sdk);
        }

        myInterpreterComboBox.setModel(new CollectionComboBoxModel<Sdk>(sdkList, selection));
    }

    public Module getModule() {
        return myModuleComboBox.getSelectedModule();
    }

    public void setModule(Module module) {
        myModuleComboBox.setSelectedModule(module);
    }

    public boolean isUseModuleSdk() {
        return myUseModuleSdkRadioButton.isSelected();
    }

    public void setUseModuleSdk(boolean useModuleSdk) {
        if (useModuleSdk) {
            myUseModuleSdkRadioButton.setSelected(true);
        }
        else {
            myUseSpecifiedSdkRadioButton.setSelected(true);
        }
        updateControls();
    }
}
