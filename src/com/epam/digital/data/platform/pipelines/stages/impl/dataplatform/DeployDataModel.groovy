/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "deploy-data-model", buildTool = "any", type = [ProjectType.LIBRARY])
class DeployDataModel {
    BuildContext context

    void run() {
        if (context.registryRegulations.deployStatus("deploy-data-model",
                "${RegulationType.DATA_MODEL.value}")) {
            try {
                context.logger.info("Deploying data model")
                context.script.build job: "${context.codebase.name}/MASTER-Build-${context.codebase.name}-data-model",
                        wait: true, propagate: true
            } catch (any) {
                context.logger.info("Data model deploy has been failed")
                context.stageFactory.runStage(context.RESTORE_STAGE, context)
            }
            context.registryRegulations.getChangedStatusOrFiles("save", "deploy-data-model",
                    "--file ${context.getWorkDir()}/${RegulationType.DATA_MODEL.value}")
        } else {
            context.logger.info("Skip ${RegulationType.DATA_MODEL.value} deploying due to no changes")
        }
    }
}
