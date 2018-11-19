package org.jf.fusionIdea.facet;

import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nls.Capitalization;
import org.jetbrains.annotations.NotNull;

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
            configuration = new FusionFacetConfiguration();
        }

        reset();
    }

    @Override public boolean isModified() {
        return !StringUtil.equals(fusionPathCombo.getText(), configuration.getFusionPath());
    }

    @Override public void apply() throws ConfigurationException {
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
