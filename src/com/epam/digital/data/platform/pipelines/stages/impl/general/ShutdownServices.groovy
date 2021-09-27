package com.epam.digital.data.platform.pipelines.stages.impl.general

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "shutdown-services", buildTool = ["any"], type = [ProjectType.LIBRARY])
class ShutdownServices {
    BuildContext context

    void run() {
        context.logger.info("Shutdown services stage")
        context.logger.warn("Shutdown services stage is not yet implemented")
    }
}
