package org.jf.fusionIdea.run;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.PanelWithAnchor;
import com.jetbrains.extensions.python.FileChooserDescriptorExtKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class FusionRunConfigurationEditor extends SettingsEditor<FusionRunConfiguration> implements PanelWithAnchor {

    private JPanel panel;
    private JComponent anchor;
    private TextFieldWithBrowseButton scriptTextField;

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
    }

    protected void resetEditorFrom(@NotNull FusionRunConfiguration configuration) {
        setScript(configuration.getScript());
    }

    protected void applyEditorTo(@NotNull FusionRunConfiguration configuration) throws ConfigurationException {
        configuration.setScript(getScript());
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
}
