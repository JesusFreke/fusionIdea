package org.jf.fusionIdea.facet;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FusionFacetConfiguration implements FacetConfiguration,
        PersistentStateComponent<FusionFacetConfiguration.State> {
    private State state = new State();

    @Override
    public FacetEditorTab[] createEditorTabs(
            FacetEditorContext editorContext, FacetValidatorsManager validatorsManager) {
        return new FacetEditorTab[0];
    }

    @Nullable @Override public FusionFacetConfiguration.State getState() {
        return state;
    }

    @Override public void loadState(@NotNull FusionFacetConfiguration.State state) {
        this.state.fusionPath = state.fusionPath;
    }

    public static class State {
        public String fusionPath;
    }
}
