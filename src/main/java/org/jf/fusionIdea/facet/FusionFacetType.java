package org.jf.fusionIdea.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.jetbrains.python.PythonModuleTypeBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FusionFacetType extends FacetType<FusionFacet, FusionFacetConfiguration> {

    public FusionFacetType() {
        super(new FacetTypeId<FusionFacet>("fusion360plugin"),
                "fusion360plugin", "Fusion 360 Plugin");
    }

    @Override public FusionFacetConfiguration createDefaultConfiguration() {
        // TODO: automatically find the fusion 360 installation
        return new FusionFacetConfiguration();
    }

    @Override
    public FusionFacet createFacet(@NotNull Module module, String name,
                                   @NotNull FusionFacetConfiguration configuration, @Nullable Facet underlyingFacet) {
        return new FusionFacet(this, module, name, configuration, underlyingFacet);
    }

    @Override public boolean isSuitableModuleType(ModuleType moduleType) {
        return moduleType instanceof PythonModuleTypeBase;
    }
}
