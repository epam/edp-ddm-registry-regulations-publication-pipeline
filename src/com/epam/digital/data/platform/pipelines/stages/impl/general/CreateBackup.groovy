package com.epam.digital.data.platform.pipelines.stages.impl.general

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "create-backup", buildTool = ["any"], type = [ProjectType.LIBRARY])
class CreateBackup {
    BuildContext context

    void run() {
        context.logger.info("Create backup stage")
        context.logger.warn("Create backup stage is not yet implemented")
    }
}
