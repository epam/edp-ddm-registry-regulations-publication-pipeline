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
                    context.logger.info("Commiting and pushing changes to ${dataComponent.name}")
                    context.gitClient.gitSetConfig()
                    context.gitClient.gitAdd()
                    context.gitClient.gitCommit(COMMIT_MESSAGE)
                    context.gitClient.gitPush(context.codebase.branch)
                }
            }
        }
    }
}
