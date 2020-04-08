/*
 * Copyright 2020, Ben Gruver
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

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import com.jetbrains.python.debugger.PyDebugProcess;
import com.jetbrains.python.debugger.PyRemoteDebugProcess;
import com.jetbrains.python.debugger.PyRemoteDebugProcessAware;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class FusionDebugProcessHandler extends ProcessHandler implements PyRemoteDebugProcessAware {
    private final Project project;
    private final AtomicBoolean closedByUser = new AtomicBoolean();

    @Nullable
    private PyDebugProcess debugProcess;

    public FusionDebugProcessHandler(Project project) {
        this.project = project;
    }

    protected void destroyProcessImpl() {
        if (debugProcess != null) {
            debugProcess.stop();
        }
        this.detachProcessImpl();
    }

    protected void detachProcessImpl() {
        this.notifyProcessTerminated(0);
        this.notifyTextAvailable("Server stopped.\n", ProcessOutputTypes.SYSTEM);
    }

    @Override public boolean detachIsDefault() {
        return false;
    }

    @Nullable @Override public OutputStream getProcessInput() {
        return null;
    }

    @Override public void setRemoteDebugProcess(PyRemoteDebugProcess pyRemoteDebugProcess) {
        this.debugProcess = pyRemoteDebugProcess;
    }
}
