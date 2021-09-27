package com.epam.digital.data.platform.pipelines.stages.impl.general

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "run-autotests", buildTool = ["any"], type = [ProjectType.LIBRARY])
class RunAutotests {
    BuildContext context

    void run() {
        try {
            context.logger.info("Run autotests stage")
            context.logger.warn("Run autotests stage is not yet implemented")
        } catch (any) {
            context.stageFactory.runStage(context.RESTORE_STAGE, context)
        }
    }
}
