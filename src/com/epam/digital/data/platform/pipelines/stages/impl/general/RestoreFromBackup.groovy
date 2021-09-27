package com.epam.digital.data.platform.pipelines.stages.impl.general

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "restore-from-backup", buildTool = ["any"], type = [ProjectType.LIBRARY])
class RestoreFromBackup {
    BuildContext context

    void run() {
        context.logger.info("Restore from backup stage")
        context.script.error("Previous version of the registry has been restored")
    }
}
