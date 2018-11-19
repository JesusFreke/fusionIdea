package org.jf.fusionIdea.attach;

import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.xdebugger.attach.XAttachProcessPresentationGroup;
import icons.PythonIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class FusionLocalAttachGroup implements XAttachProcessPresentationGroup {
    public static final FusionLocalAttachGroup INSTANCE = new FusionLocalAttachGroup();

    public FusionLocalAttachGroup() {
    }

    @Override public int getOrder() {
        return 0;
    }

    @NotNull @Override public String getGroupName() {
        return "Fusion 360 Processes";
    }

    @NotNull @Override
    public Icon getProcessIcon(@NotNull Project project, @NotNull ProcessInfo info, @NotNull UserDataHolder dataHolder) {
        return getItemIcon(project, info, dataHolder);
    }

    @NotNull @Override
    public String getProcessDisplayText(@NotNull Project project, @NotNull ProcessInfo info, @NotNull UserDataHolder dataHolder) {
        return getItemDisplayText(project, info, dataHolder);
    }

    @NotNull @Override
    public Icon getItemIcon(@NotNull Project project, @NotNull ProcessInfo info, @NotNull UserDataHolder dataHolder) {
        return PythonIcons.Python.Python;
    }

    @NotNull @Override
    public String getItemDisplayText(@NotNull Project project, @NotNull ProcessInfo info, @NotNull UserDataHolder dataHolder) {
        return info.getExecutableDisplayName();
    }

    @Nullable @Override
    public String getItemDescription(@NotNull Project project, @NotNull ProcessInfo info, @NotNull UserDataHolder dataHolder) {
        return "Fusion 360";
    }
}
