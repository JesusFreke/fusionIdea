package org.jf.fusionIdea.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.jetbrains.python.run.PythonConfigurationFactoryBase;
import icons.PythonIcons.Python;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class FusionRunConfigurationType implements ConfigurationType {
  public final FusionRunConfigurationFactory FACTORY = new FusionRunConfigurationFactory(this);

  public static FusionRunConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(FusionRunConfigurationType.class);
  }

  public String getDisplayName() {
    return "Fusion 360 Python Script";
  }

  public String getConfigurationTypeDescription() {
    return "Fusion 360 Python Run Configuration";
  }

  public Icon getIcon() {
    // TODO: fusion 360 icon?
    return Python.Python;
  }

  @NotNull
  public String getId() {
    return "Fusion360PythonScript";
  }

  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[] { FACTORY };
  }

  private static class FusionRunConfigurationFactory extends PythonConfigurationFactoryBase {
    public FusionRunConfigurationFactory(@NotNull ConfigurationType type) {
      super(type);
    }

    @Override
    public boolean isConfigurationSingletonByDefault() {
      return false;
    }

    @Override public boolean canConfigurationBeSingleton() {
      return false;
    }

    @NotNull
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
      return new FusionRunConfiguration(project, this);
    }

    @Override
    public String getName() {
      return "Fusion 360 Python Script";
    }
  }
}
