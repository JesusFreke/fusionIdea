package org.jf.fusionIdea.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.ui.DefaultFacetSettingsEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.python.PythonModuleTypeBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.fusionIdea.FusionIdeaIcons;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class FusionFacetType extends FacetType<FusionFacet, FusionFacetConfiguration> {

    private static final String STRING_ID = "fusion360plugin";
    public static final FacetTypeId<FusionFacet> ID = new FacetTypeId<FusionFacet>(STRING_ID);

    public FusionFacetType() {
        super(ID, STRING_ID, "Fusion 360 Support");
    }

    @Nullable @Override public Icon getIcon() {
        return FusionIdeaIcons.LOGO;
    }

    public static FusionFacetType getInstance() {
        return findInstance(FusionFacetType.class);
    }

    public static FusionFacetConfiguration newDefaultConfiguration() {
        VirtualFile homeDir = VfsUtil.getUserHomeDir();

        File startPath = new File(homeDir.getCanonicalPath(), "AppData/Local/Autodesk/webdeploy/production");

        File fusionPath = null;

        if (startPath.exists()) {
            for (File subdir : startPath.listFiles(File::isDirectory)) {
                File candidatePath = new File(subdir, "Fusion360.exe");
                if (candidatePath.exists()) {
                    fusionPath = candidatePath;
                    break;
                }
            }
        }

        if (fusionPath != null) {
            try {
                return new FusionFacetConfiguration(fusionPath.getCanonicalPath());
            } catch (IOException e) {
            }
            return new FusionFacetConfiguration(fusionPath.getAbsolutePath());
        }
        return new FusionFacetConfiguration(null);
    }

    @Override public FusionFacetConfiguration createDefaultConfiguration() {
        return newDefaultConfiguration();
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
