package com.epam.digital.data.platform.pipelines.stages.impl.general

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "get-changes", buildTool = ["any"], type = [ProjectType.LIBRARY])
class GetChanges {
    BuildContext context

    void run() {
        RegulationType.values().each {
            context.registryRegulations.filesToDeploy.put(it, context.registryRegulations.getChangedRegulations(it))
        }
    }
}
