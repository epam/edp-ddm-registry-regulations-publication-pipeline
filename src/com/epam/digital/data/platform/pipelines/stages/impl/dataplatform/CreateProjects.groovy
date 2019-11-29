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

@Stage(name = "create-projects", buildTool = ["any"], type = [ProjectType.LIBRARY])
class CreateProjects {
    BuildContext context

    void run() {
        context.dataComponents.values().each { dataComponent ->
            dataComponent.setWorkDir("${context.workDir}/${dataComponent.name}")
            context.script.dir(dataComponent.getWorkDir()) {
                dataComponent.createCodebaseCR()
                dataComponent.createCodebaseBranchCR()
            }
        }
        checkAllReposCreated()
    }

    void checkAllReposCreated(int maxAttempts = 50, int checkInterval = 5) {
        context.logger.info("Check if all repositories have been created")
        boolean isAllReposCreated = false
        int currentAttempt = 0
        while (!isAllReposCreated) {
            currentAttempt++
            context.logger.info("Attempt ${currentAttempt} of ${maxAttempts}")
            if (currentAttempt > maxAttempts)
                context.script.error("Attempts limit is reached and repositories have not been created yet")
            isAllReposCreated = true
            context.dataComponents.values().each { dataComponent ->
                if (!dataComponent.getIsRepositoryCreated())
                    if (context.gitServer.isRepositoryExists(dataComponent.codebaseName)) {
                        dataComponent.setIsRepositoryCreated(true)
                        context.logger.info("${dataComponent.codebaseName} repository has been created")
                    } else {
                        context.logger.info("${dataComponent.codebaseName} repository is not created yet")
                        isAllReposCreated = false
                    }
            }
            if (!isAllReposCreated)
                context.script.sleep(checkInterval)
        }
    }
}
