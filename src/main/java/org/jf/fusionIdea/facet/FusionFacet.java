package org.jf.fusionIdea.facet;

import com.intellij.execution.process.ProcessInfo;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.impl.libraries.LibraryEx.ModifiableModelEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.attach.LocalAttachHost;
import com.jetbrains.python.facet.LibraryContributingFacet;
import com.jetbrains.python.library.PythonLibraryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
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

    public static boolean hasFacet(Module module) {
        return getInstance(module) != null;
    }

    @Nullable
    private static VirtualFile getFusionPathVirtualFile(String fusionPath) {
        if (fusionPath == null) {
            return null;
        }
        File fusionExecutable = new File(fusionPath);
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
