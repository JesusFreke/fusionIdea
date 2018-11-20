package org.jf.fusionIdea.facet;

import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileTypeDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nls.Capitalization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class FusionFacetEditorTab extends FacetEditorTab {

    private final FusionFacetConfiguration configuration;
    private Module module;
    private FacetEditorContext editorContext;

    private TextFieldWithBrowseButton fusionPathCombo;
    private JPanel panel;

    public FusionFacetEditorTab(FacetEditorContext editorContext) {
        this.editorContext = editorContext;
        this.module = editorContext.getModule();

        FusionFacet facet = FusionFacet.getInstance(module);
        if (facet != null) {
            configuration = facet.getConfiguration();
        } else {
            configuration = FusionFacetType.newDefaultConfiguration();
        }

        FileChooserDescriptor chooserDescriptor = new FileTypeDescriptor("Choose Fusion 360 executable", ".exe");

        TextBrowseFolderListener listener =
                new TextBrowseFolderListener(chooserDescriptor, editorContext.getProject()) {
                    @Nullable @Override protected VirtualFile getInitialFile() {
                        VirtualFile initialFile = super.getInitialFile();
                        if (initialFile != null) {
                            return initialFile;
                        }

                        return VfsUtil.getUserHomeDir();
                    }
                };
        fusionPathCombo.addBrowseFolderListener(listener);
        reset();
    }

    @Override public boolean isModified() {
        return !StringUtil.equals(fusionPathCombo.getText(), configuration.getFusionPath());
    }

    @Override public void apply() throws ConfigurationException {
        String fusionPath = fusionPathCombo.getText();

        if (!FusionFacet.checkFusionPath(fusionPath)) {
            throw new ConfigurationException("Can't locate the Fusion APIs from the given path. Expecting the path " +
                    "to the Fusion360.exe executable");
        }

        configuration.setFusionPath(fusionPathCombo.getText());

        FusionFacet facet = FusionFacet.getInstance(module);
        if (facet != null) {
            facet.updateLibrary(editorContext);
        }
    }

    @Override public void reset() {
        this.fusionPathCombo.setText(configuration.getFusionPath());
    }

    @NotNull @Override public JComponent createComponent() {
        return panel;
    }

    @Nls(capitalization = Capitalization.Title) @Override public String getDisplayName() {
        return "Fusion 360 Plugin Configuration";
    }
}
