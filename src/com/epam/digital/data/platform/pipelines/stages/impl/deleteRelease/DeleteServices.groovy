package com.epam.digital.data.platform.pipelines.stages.impl.deleteRelease

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage
import com.epam.digital.data.platform.pipelines.tools.Helm

@Stage(name = "delete-services", buildTool = ["any"], type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class DeleteServices {
    BuildContext context

    void run() {
        context.logger.info("Removing ${context.codebase.name} helm release")
        Helm.uninstall(context, context.codebase.name.substring(0, context.codebase.name.length() - 6),
                context.namespace, true)

        context.logger.info("Removing ${context.codebase.name} repo")
        context.gitServer.deleteRepository(context.codebase.name)
    }
}
