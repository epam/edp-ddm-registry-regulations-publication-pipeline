/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.pipelines.stages.impl.general

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "create-branch", buildTool = ["any"], type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class CreateBranch {
    BuildContext context

    void run() {
        context.script.sshagent(["${context.gitServer.credentialsId}"]) {
            try {
                context.gitClient.gitSetConfig()
                context.script.sh(script: "git branch ${context.getParameterValue("BRANCH")}")
                context.gitClient.gitPush()
            }
            catch (any) {
                context.script.error "Create branch has failed with exception"
            }
        }
    }
}
