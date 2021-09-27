package com.epam.digital.data.platform.pipelines.stages.impl.general

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "checkout", buildTool = ["any"], type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class Checkout {
    BuildContext context

    void run() {
        context.logger.info("Checkout registry regulations")
        context.gitClient.checkout(context.codebase.repositoryPath, context.codebase.branch,
                context.gitServer.credentialsId)
    }
}
