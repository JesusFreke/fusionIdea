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

package org.jf.fusionIdea.facet;

import com.intellij.execution.process.ProcessInfo;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.impl.libraries.LibraryEx.ModifiableModelEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.attach.LocalAttachHost;
import com.jetbrains.python.facet.LibraryContributingFacet;
import com.jetbrains.python.library.PythonLibraryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FusionFacet extends LibraryContributingFacet<FusionFacetConfiguration> {

    private static final String LIB_NAME = "Fusion 360 API";

    public FusionFacet(@NotNull FacetType facetType, @NotNull Module module, @NotNull String name,
                       @NotNull FusionFacetConfiguration configuration, Facet underlyingFacet) {
        super(facetType, module, name, configuration, underlyingFacet);
    }

    @Nullable public static FusionFacet getInstance(Module module) {
        return FacetManager.getInstance(module).getFacetByType(FusionFacetType.ID);
    }

    public void updateLibrary(FacetEditorContext editorContext) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            ModifiableRootModel rootModel = editorContext.getModifiableRootModel();
            updateLibrary(rootModel);
        });
    }

    public static FusionFacet addFacet(Module module, ModifiableModelsProvider modifiableModelsProvider) {
        return ApplicationManager.getApplication().runWriteAction(new Computable<FusionFacet>() {
            @Override public FusionFacet compute() {
                ModifiableFacetModel facetModel = modifiableModelsProvider.getFacetModifiableModel(module);
                FusionFacetType fusionFacetType = FusionFacetType.getInstance();
                FusionFacet fusionFacet = FacetManager.getInstance(module).createFacet(fusionFacetType,
                        fusionFacetType.getDefaultFacetName(), null);
                facetModel.addFacet(fusionFacet);
                modifiableModelsProvider.commitFacetModifiableModel(module, facetModel);
                return fusionFacet;
            }
        });
    }

    public void removeFacet(ModifiableModelsProvider modifiableModelsProvider) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            ModifiableFacetModel facetModel = modifiableModelsProvider.getFacetModifiableModel(getModule());
            facetModel.removeFacet(this);
            modifiableModelsProvider.commitFacetModifiableModel(getModule(), facetModel);
        });
    }

    public static boolean hasFacet(Module module) {
        return getInstance(module) != null;
    }

    public static String autoDetectFusionPath() {
        VirtualFile homeDir = VfsUtil.getUserHomeDir();
        if (homeDir == null) {
            return null;
        }

        File startPath = new File(homeDir.getCanonicalPath(), "AppData/Local/Autodesk/webdeploy/production");

        if (startPath.exists()) {
            for (File subdir : startPath.listFiles(File::isDirectory)) {
                File candidatePath = new File(subdir, "Fusion360.exe");
                if (candidatePath.exists()) {
                    try {
                        String canonicalPath = candidatePath.getCanonicalPath();
                        if (checkFusionPath(canonicalPath)) {
                            return canonicalPath;
                        }
                    } catch (IOException ex) {
                        // ignore and continue
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private static VirtualFile getFusionPathVirtualFile(String fusionPath) {
        if (fusionPath == null) {
            return null;
        }
        File fusionExecutable = new File(fusionPath);
        VirtualFile executableVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(fusionExecutable);
        if (executableVirtualFile == null || ! executableVirtualFile.exists()) {
            return null;
        }

        File apiLocation = new File(fusionExecutable.getParentFile(), "Api/Python/packages/adsk/defs");
        VirtualFile apiVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(apiLocation);

        if (apiVirtualFile == null || !apiVirtualFile.exists()) {
            return null;
        }
        return apiVirtualFile;
    }

    public static boolean checkFusionPath(String fusionPath) {
        return getFusionPathVirtualFile(fusionPath) != null;
    }

    private void updateLibrary(ModifiableRootModel model) {
        VirtualFile apiVirtualFile = getFusionPathVirtualFile(getConfiguration().getFusionPath());

        if (apiVirtualFile == null) {
            removeLibrary();
            return;
        }

        LibraryTable libraryTable = model.getModuleLibraryTable();
        LibraryEx library = (LibraryEx) libraryTable.getLibraryByName(LIB_NAME);
        if (library != null) {
            libraryTable.removeLibrary(library);
        }
        library = (LibraryEx) libraryTable.createLibrary(LIB_NAME);
        ModifiableModelEx libraryModel = library.getModifiableModel();
        libraryModel.setKind(PythonLibraryType.getInstance().getKind());
        libraryModel.addRoot(apiVirtualFile, OrderRootType.CLASSES);
        libraryModel.commit();

        LibraryOrderEntry entry = model.findLibraryOrderEntry(library);
        entry.setScope(DependencyScope.PROVIDED);
        entry.setExported(false);
    }

    @Override public void updateLibrary() {
        ApplicationManager.getApplication().runWriteAction(() -> {
            ModifiableRootModel model = ModuleRootManager.getInstance(getModule()).getModifiableModel();
            updateLibrary(model);
            model.commit();
        });
    }

    @Override public void removeLibrary() {
        ApplicationManager.getApplication().runWriteAction(() -> {
            ModifiableRootModel model = ModuleRootManager.getInstance(getModule()).getModifiableModel();
            LibraryTable libraryTable = model.getModuleLibraryTable();
            Library library = libraryTable.getLibraryByName(LIB_NAME);
            if (library != null) {
                libraryTable.removeLibrary(library);
            }
            model.commit();
        });
    }

    @Override public void initFacet() {
        updateLibrary();
    }

    public List<ProcessInfo> findTargetProcesses() {
        List<ProcessInfo> targetProcesses = new ArrayList<>();
        String fusionPath = this.getConfiguration().getFusionPath();
        if (fusionPath == null) {
            return targetProcesses;
        }

        for (ProcessInfo processInfo : LocalAttachHost.INSTANCE.getProcessList()) {
            if (StringUtil.containsIgnoreCase(processInfo.getCommandLine(), fusionPath)) {
                targetProcesses.add(processInfo);
            }
        }

        return targetProcesses;
    }
}
