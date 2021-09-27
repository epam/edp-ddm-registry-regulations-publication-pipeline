package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "deploy-data-model", buildTool = "any", type = [ProjectType.LIBRARY])
class DeployDataModel {
    BuildContext context

    void run() {
        try {
            if (context.registryRegulations.filesToDeploy.get(RegulationType.DATA_MODEL)) {
                context.logger.info("Deploying data model")
                context.script.build job: "${context.codebase.name}/MASTER-Build-${context.codebase.name}-data-model",
                        wait: true, propagate: true
            } else {
                context.logger.info("Skip ${RegulationType.DATA_MODEL.value} data model deploying due to no changes")
            }
        } catch (any) {
            context.logger.info("Data model deploy has been failed")
            context.stageFactory.runStage(context.RESTORE_STAGE, context)
        }
    }
}
