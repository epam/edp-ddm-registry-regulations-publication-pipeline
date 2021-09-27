package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "clone-projects", buildTool = ["any"], type = [ProjectType.LIBRARY])
class CloneProjects {
    BuildContext context

    void run() {
        context.dataComponents.values().each { dataComponent ->
            context.script.dir(dataComponent.getWorkDir()) {
                context.logger.info("Checkout ${dataComponent.name}")
                context.gitClient.checkout(dataComponent.repositoryPath, dataComponent.codebaseBranch,
                        context.gitServer.credentialsId)
                context.script.sh(script: "git checkout -b ${dataComponent.codebaseBranch}")
            }
        }
    }
}
