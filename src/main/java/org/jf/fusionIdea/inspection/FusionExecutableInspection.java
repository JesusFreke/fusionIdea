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

package org.jf.fusionIdea.inspection;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.python.inspections.PyInspection;
import com.jetbrains.python.inspections.PyInspectionVisitor;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.types.TypeEvalContext;
import com.jetbrains.python.sdk.PythonSdkUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.fusionIdea.FusionIdeaPlugin;
import org.jf.fusionIdea.facet.FusionFacet;
import org.jf.fusionIdea.facet.FusionFacetType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FusionExecutableInspection extends PyInspection {
    @Nls @NotNull @Override public String getDisplayName() {
        return "Invalid Fusion 360 path selected";
    }

    protected List<LocalQuickFix> getQuickFixesForUpdatedFusionExecutable(@Nullable String newPath) {
        List<LocalQuickFix> fixes = new ArrayList<>();
        if (newPath != null) {
            fixes.add(new UpdateFusionExecutableInAllModules(newPath));
            fixes.add(new UpdateFusionExecutableInModule(newPath));
        }
        fixes.add(new OpenFacetConfiguration());
        return fixes;
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder,
                                          final boolean isOnTheFly,
                                          @NotNull final LocalInspectionToolSession session) {
        return new Visitor(holder, PyInspectionVisitor.getContext(session));
    }

    public class Visitor extends PyInspectionVisitor {

        public Visitor(@Nullable ProblemsHolder holder,
                       @NotNull TypeEvalContext context) {
            super(holder, context);
        }

        @Override
        public void visitPyFile(PyFile node) {
            final Module module = ModuleUtilCore.findModuleForPsiElement(node);
            if (module == null) {
                return;
            }

            FusionFacet facet = FusionFacet.getInstance(module);
            if (facet == null) {
                return;
            }

            if (!FusionFacet.checkFusionPath(facet.getConfiguration().getFusionPath())) {
                List<LocalQuickFix> fixes;
                String newPath = FusionFacet.autoDetectFusionPath();

                fixes = getQuickFixesForUpdatedFusionExecutable(newPath);

                registerProblem(node, "The configured Fusion 360 executable is invalid.",
                        fixes.toArray(LocalQuickFix.EMPTY_ARRAY));
            }
        }
    }

    public class UpdateFusionExecutableInAllModules implements LocalQuickFix {
        private final String newPath;

        public UpdateFusionExecutableInAllModules(String newPath) {
            this.newPath = newPath;
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return "Update Fusion 360 executable in all modules";
        }

        @Override
        public boolean startInWriteAction() {
            return false;
        }

        @Override
        public void applyFix(@NotNull final Project project, @NotNull ProblemDescriptor descriptor) {

            ModuleManager moduleManager = ModuleManager.getInstance(project);
            for (Module module : moduleManager.getModules()) {
                ModifiableFacetModel facetModel = FacetManager.getInstance(module).createModifiableModel();
                FusionFacet facet = facetModel.getFacetByType(FusionFacetType.ID);
                if (facet == null) {
                    continue;
                }

                ApplicationManager.getApplication().runWriteAction(() -> {
                    facet.getConfiguration().setFusionPath(newPath);
                    facet.updateLibrary();

                    Sdk sdk = PythonSdkUtil.findPythonSdk(module);
                    if (sdk != null && FusionFacet.getFusionSubPath(sdk.getHomePath()) != null && !new File(sdk.getHomePath()).exists()) {
                        updateSdk(sdk, newPath);
                    }
                    facetModel.commit();
                });
            }
        }
    }

    public final class UpdateFusionExecutableInModule implements LocalQuickFix {
        private final String newPath;

        public UpdateFusionExecutableInModule(String newPath) {
            this.newPath = newPath;
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return "Update Fusion 360 executable in current module";
        }

        @Override
        public boolean startInWriteAction() {
            return false;
        }

        @Override
        public void applyFix(@NotNull final Project project, @NotNull ProblemDescriptor descriptor) {
            Module module = ModuleUtilCore.findModuleForPsiElement(descriptor.getPsiElement());
            if (module == null) {
                return;
            }

            ModifiableFacetModel facetModel = FacetManager.getInstance(module).createModifiableModel();
            FusionFacet facet = facetModel.getFacetByType(FusionFacetType.ID);
            if (facet == null) {
                return;
            }
            ApplicationManager.getApplication().runWriteAction(() -> {
                facet.getConfiguration().setFusionPath(newPath);
                facet.updateLibrary();

                Sdk sdk = PythonSdkUtil.findPythonSdk(module);
                if (FusionFacet.getFusionSubPath(sdk.getHomePath()) != null && !sdk.getHomeDirectory().exists()) {
                    updateSdk(sdk, newPath);
                }
                facetModel.commit();
            });
        }
    }

    public class OpenFacetConfiguration implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Open Fusion 360 facet configuration";
        }

        @Override
        public boolean startInWriteAction() {
            return false;
        }

        @Override
        public void applyFix(@NotNull final Project project, @NotNull ProblemDescriptor descriptor) {
            Module module = ModuleUtilCore.findModuleForPsiElement(descriptor.getPsiElement());
            if (module == null) {
                return;
            }
            FusionFacet facet = FusionFacet.getInstance(module);

            if (facet != null) {
                ProjectStructureConfigurable configurable = ProjectStructureConfigurable.getInstance(project);
                ShowSettingsUtil.getInstance().editConfigurable(project, configurable);
                configurable.select(facet, true);
            }
        }
    }

    private static void updateSdk(Sdk sdk, String newFusionPath) {
        File baseFusionDirectory = new File(newFusionPath).getParentFile();

        SdkModificator modificator = sdk.getSdkModificator();

        modificator.setHomePath(
                new File(baseFusionDirectory, "Python/python.exe").getAbsolutePath());

        for (OrderRootType rootType : OrderRootType.getAllTypes()) {
            replaceSdkRoots(modificator, rootType, baseFusionDirectory);
        }

        modificator.commitChanges();
    }

    private static void replaceSdkRoots(SdkModificator modificator, OrderRootType rootType, File baseFusionDirectory) {
        for (String rootUrl : modificator.getUrls(rootType)) {
            try {
                File root = new File(FileUtilRt.toSystemDependentName(VirtualFileManager.extractPath(rootUrl)));
                String subPath = FusionFacet.getFusionSubPath(root.getCanonicalPath());
                if (subPath != null) {
                    modificator.removeRoot(rootUrl, rootType);

                    VirtualFile newPath = LocalFileSystem.getInstance().findFileByIoFile(new File(baseFusionDirectory, subPath));
                    if (newPath != null) {
                        modificator.addRoot(
                                newPath,
                                rootType);
                    }
                }
            } catch (Exception ex) {
                FusionIdeaPlugin.log.error(ex);
            }
        }
    }
}
