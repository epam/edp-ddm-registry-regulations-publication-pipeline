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
