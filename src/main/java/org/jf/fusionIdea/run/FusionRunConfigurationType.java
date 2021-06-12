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

package org.jf.fusionIdea.run;

import com.intellij.execution.configurations.*;
import com.intellij.openapi.project.Project;
import com.jetbrains.python.run.PythonConfigurationFactoryBase;
import org.jetbrains.annotations.NotNull;
import org.jf.fusionIdea.FusionIdeaIcons;

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
    return FusionIdeaIcons.LOGO;
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

    @NotNull @Override public RunConfigurationSingletonPolicy getSingletonPolicy() {
      return RunConfigurationSingletonPolicy.SINGLE_INSTANCE;
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
