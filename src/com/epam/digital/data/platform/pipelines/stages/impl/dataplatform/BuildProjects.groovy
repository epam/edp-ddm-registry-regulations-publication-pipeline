package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registrycomponents.generated.DataComponent
import com.epam.digital.data.platform.pipelines.registrycomponents.generated.DataComponentType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "build-projects", buildTool = ["any"], type = [ProjectType.LIBRARY])
class BuildProjects {
    BuildContext context

    void run() {
        context.logger.info("Building the data model first")
        DataComponent dataModel = context.dataComponents.get(DataComponentType.MODEL.getValue())
        context.script.build job: dataModel.pipelineName, wait: true, propagate: true

        LinkedHashMap parallelBuilds = [:]
        context.dataComponents.values().each { dataComponent ->
            parallelBuilds["${dataComponent.name}"] = {
                if (dataComponent.name != dataModel.name) {
                    context.logger.info("Building ${dataComponent.name}")
                    context.script.build job: dataComponent.pipelineName, wait: true, propagate: true
                }
            }
        }
        context.script.parallel(parallelBuilds)
    }
}
