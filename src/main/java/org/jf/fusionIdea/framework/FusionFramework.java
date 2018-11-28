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

package org.jf.fusionIdea.framework;

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
                        FusionFacet.addFacet(module, modifiableModelsProvider);
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
