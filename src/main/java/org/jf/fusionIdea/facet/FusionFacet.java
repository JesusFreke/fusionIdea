package org.jf.fusionIdea.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.openapi.module.Module;
import com.jetbrains.python.facet.LibraryContributingFacet;
import org.jetbrains.annotations.NotNull;

public class FusionFacet extends LibraryContributingFacet<FusionFacetConfiguration> {

    public FusionFacet(@NotNull FacetType facetType, @NotNull Module module, @NotNull String name,
                       @NotNull FusionFacetConfiguration configuration, Facet underlyingFacet) {
        super(facetType, module, name, configuration, underlyingFacet);
    }

    @Override public void updateLibrary() {

    }

    @Override public void removeLibrary() {

    }
}
