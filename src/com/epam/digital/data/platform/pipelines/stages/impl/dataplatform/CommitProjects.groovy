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

package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "commit-projects", buildTool = ["any"], type = [ProjectType.LIBRARY])
class CommitProjects {
    BuildContext context

    private final String COMMIT_MESSAGE = "Service generated"

    void run() {
        context.dataComponents.values().each { dataComponent ->
            context.script.dir(dataComponent.getWorkDir()) {
                context.script.sshagent(["${context.gitServer.credentialsId}"]) {
                    context.logger.info("Commiting and pushing changes to ${dataComponent.fullName}")
                    context.gitClient.gitSetConfig()
                    context.gitClient.gitAdd()
                    context.gitClient.gitCommit(COMMIT_MESSAGE)
                    context.script.retry(5) {
                        context.gitClient.gitPush(dataComponent.codebaseBranch)
                    }
                }
            }
        }
    }
}
