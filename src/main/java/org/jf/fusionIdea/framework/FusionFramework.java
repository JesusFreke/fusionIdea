package org.jf.fusionIdea.framework;

import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.framework.FrameworkTypeEx;
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.jetbrains.python.PythonModuleTypeBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.fusionIdea.FusionIdeaIcons;
import org.jf.fusionIdea.facet.FusionFacet;
import org.jf.fusionIdea.facet.FusionFacetType;

import javax.swing.*;

public class FusionFramework extends FrameworkTypeEx {
    public FusionFramework() {
        super("fusionIdea");
    }

    @NotNull @Override public FrameworkSupportInModuleProvider createProvider() {
        return new FrameworkSupportInModuleProvider() {
            @NotNull @Override public FrameworkTypeEx getFrameworkType() {
                return FusionFramework.this;
            }

            @NotNull @Override
            public FrameworkSupportInModuleConfigurable createConfigurable(@NotNull FrameworkSupportModel model) {
                return new FrameworkSupportInModuleConfigurable() {
                    @Nullable @Override public JComponent createComponent() {
                        return null;
                    }

                    @Override
                    public void addSupport(@NotNull Module module, @NotNull ModifiableRootModel rootModel,
                                           @NotNull ModifiableModelsProvider modifiableModelsProvider) {
                        ModifiableFacetModel facetModel = modifiableModelsProvider.getFacetModifiableModel(module);
                        FusionFacetType fusionFacetType = FusionFacetType.getInstance();
                        FusionFacet fusionFacet = FacetManager.getInstance(module).createFacet(fusionFacetType,
                                fusionFacetType.getDefaultFacetName(), null);
                        facetModel.addFacet(fusionFacet);
                        modifiableModelsProvider.commitFacetModifiableModel(module, facetModel);
                    }
                };
            }

            @Override public boolean isEnabledForModuleType(@NotNull ModuleType moduleType) {
                // The only purpose of this framework is to give a convenient way to add the fusion facet during
                // module creation. If we allow it for existing modules, the user would see both the framework and
                // facet in the "add facets" thing in the module settings.
                return false;
            }

            @Override public boolean isEnabledForModuleBuilder(@NotNull ModuleBuilder builder) {
                return builder.getModuleType() instanceof PythonModuleTypeBase;
            }
        };
    }

    @NotNull @Override public String getPresentableName() {
        return "Fusion 360 Support";
    }

    @NotNull @Override public Icon getIcon() {
        return FusionIdeaIcons.LOGO;
    }
}
