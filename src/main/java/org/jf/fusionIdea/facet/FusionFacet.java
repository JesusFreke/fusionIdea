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

import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.impl.libraries.LibraryEx.ModifiableModelEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.attach.LocalAttachHost;
import com.jetbrains.python.facet.LibraryContributingFacet;
import com.jetbrains.python.library.PythonLibraryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.fusionIdea.FusionIdeaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FusionFacet extends LibraryContributingFacet<FusionFacetConfiguration> {

    private static final String LIB_NAME = "Fusion 360 API";

    private long latestAddinVersionTimestampNanos;
    private Float latestAddinVersion;

    public FusionFacet(@NotNull FacetType facetType, @NotNull Module module, @NotNull String name,
                       @NotNull FusionFacetConfiguration configuration, Facet underlyingFacet) {
        super(facetType, module, name, configuration, underlyingFacet);
    }

    @Nullable public static FusionFacet getInstance(Module module) {
        return FacetManager.getInstance(module).getFacetByType(FusionFacetType.ID);
    }

    @Nullable public static FusionFacet getInstance(Project project) {
        for (Module module: ModuleManager.getInstance(project).getModules()) {
            FusionFacet facet = FusionFacet.getInstance(module);
            if (facet != null) {
                return facet;
            }
        }
        return null;
    }

    public void updateLibrary(FacetEditorContext editorContext) {
        ApplicationManager.getApplication().runWriteAction(() -> {
            ModifiableRootModel rootModel = editorContext.getModifiableRootModel();
            updateLibrary(rootModel);
            rootModel.commit();
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

    @Nullable
    private static String autoDetectFusionPathWindows(VirtualFile homeDir) {
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

    public static String autoDetectFusionPathMac(VirtualFile homeDir) {
        File fusionExecutable = new File(homeDir.getCanonicalPath(), "Library/Application Support/Autodesk/" +
                "webdeploy/production/Autodesk Fusion 360.app/Contents/MacOS/Autodesk Fusion 360");

        if (fusionExecutable.exists()) {
            return fusionExecutable.getAbsolutePath();
        } else {
            FusionIdeaPlugin.log.warn("Couldn't find fusion executable at " + fusionExecutable.getAbsolutePath());
        }
        return null;
    }

    @Nullable
    public static String autoDetectFusionPath() {
        VirtualFile homeDir = VfsUtil.getUserHomeDir();
        if (homeDir == null) {
            FusionIdeaPlugin.log.warn("No home dir");
            return null;
        }

        if (SystemInfo.isWindows) {
            return autoDetectFusionPathWindows(homeDir);
        } else if (SystemInfo.isMac) {
            return autoDetectFusionPathMac(homeDir);
        }

        return null;
    }

    @Nullable
    public static String getFusionSubPath(String path) {
        String baseString = "\\Autodesk\\webdeploy\\production\\";
        int index = path.lastIndexOf("\\Autodesk\\webdeploy\\production\\");
        if (index == -1) {
            return null;
        }

        String remainder = path.substring(index + baseString.length());
        if (remainder.matches("^[0-9a-fA-F]{40}.*")) {
            if (remainder.length() == 40 || remainder.charAt(40) == '\\') {
                return remainder.substring(40);
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

        File apiLocation;
        if (SystemInfo.isWindows) {
            apiLocation = new File(fusionExecutable.getParentFile(), "Api/Python/packages/adsk/defs");
        } else if (SystemInfo.isMac) {
            apiLocation = new File(fusionExecutable.getParentFile(), "../Api/Python/packages/adsk/defs");
        } else {
            return null;
        }
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
        Runnable runnable = () -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                ModifiableRootModel model = ModuleRootManager.getInstance(getModule()).getModifiableModel();
                updateLibrary(model);
                model.commit();
            });
        };

        // TODO: deprecated usage, replace with isWriteIntentLockAcquired, once available per our minimum IDEA version
        if (!ApplicationManager.getApplication().isWriteThread()) {
            ApplicationManager.getApplication().invokeLater(runnable);
        } else {
            runnable.run();
        }
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

    public static List<ProcessInfo> getProcesses(Project project) {
        if (ApplicationManager.getApplication().isDispatchThread() ||
                ApplicationManager.getApplication().isReadAccessAllowed()) {
            SettableFuture<List<ProcessInfo>> future = SettableFuture.create();


            Thread t = new Thread(() -> future.set(LocalAttachHost.INSTANCE.getProcessList()));
            t.start();

            try {
                return future.get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                FusionIdeaPlugin.log.warn("error getting process list", e);
                return null;
            }

        } else {
            return LocalAttachHost.INSTANCE.getProcessList();
        }
    }

    public List<ProcessInfo> findTargetProcesses() {
        List<ProcessInfo> targetProcesses = new ArrayList<>();
        String fusionPath = this.getConfiguration().getFusionPath();
        if (fusionPath == null) {
            return targetProcesses;
        }

        String canonicalFusionPath;
        try {
            canonicalFusionPath = new File(fusionPath).getCanonicalPath();
        } catch (IOException ex) {
            canonicalFusionPath = fusionPath;
        }

        for (ProcessInfo processInfo : getProcesses(getModule().getProject())) {
            if (processInfo.getExecutableCannonicalPath().isPresent() &&
                    processInfo.getExecutableCannonicalPath().get().equals(canonicalFusionPath)) {
                targetProcesses.add(processInfo);
            } else if (StringUtil.containsIgnoreCase(processInfo.getCommandLine(), fusionPath)) {
                targetProcesses.add(processInfo);
            } else if (StringUtil.containsIgnoreCase(processInfo.getCommandLine(), canonicalFusionPath)) {
                targetProcesses.add(processInfo);
            }
        }
        return targetProcesses;
    }

    @Nullable
    public synchronized Float getLatestAddinVersion() {
        if (latestAddinVersion == null || (System.nanoTime() - latestAddinVersionTimestampNanos) >
                TimeUnit.NANOSECONDS.convert(1, TimeUnit.HOURS)) {

            // We'll immediately return null or the old value below, but then we'll asynchronously update it in the
            // background.
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                String version = null;
                try {
                    URL url = new URL("https://raw.githubusercontent.com/JesusFreke/fusion_idea_addin/master/VERSION");
                    HttpURLConnection yc = (HttpURLConnection)url.openConnection();
                    BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
                    version = CharStreams.toString(in);
                    synchronized (FusionFacet.this) {
                        latestAddinVersion = Float.parseFloat(version);
                        latestAddinVersionTimestampNanos = System.nanoTime();
                    }
                } catch (MalformedURLException ex) {
                    throw new RuntimeException(ex);
                } catch (IOException ex) {
                    FusionIdeaPlugin.log.warn("Error getting latest version of add-in", ex);
                } catch (NumberFormatException ex) {
                    FusionIdeaPlugin.log.warn("Got add-in version with unexpected format: " + version, ex);
                }
            });
        }
        return latestAddinVersion;
    }
}
