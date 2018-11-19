package org.jf.fusionIdea.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.ui.DefaultFacetSettingsEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.jetbrains.python.PythonModuleTypeBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FusionFacetType extends FacetType<FusionFacet, FusionFacetConfiguration> {

    private static final String STRING_ID = "fusion360plugin";
    public static final FacetTypeId<FusionFacet> ID = new FacetTypeId<FusionFacet>(STRING_ID);

    public FusionFacetType() {
        super(ID, STRING_ID, "Fusion 360 Plugin");
    }

    public static FusionFacetType getInstance() {
        return findInstance(FusionFacetType.class);
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

    @Nullable @Override
    public DefaultFacetSettingsEditor createDefaultConfigurationEditor(@NotNull Project project, @NotNull FusionFacetConfiguration configuration) {
        return super.createDefaultConfigurationEditor(project, configuration);
    }
}
