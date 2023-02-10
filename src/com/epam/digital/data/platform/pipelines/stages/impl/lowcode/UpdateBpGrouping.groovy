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

package com.epam.digital.data.platform.pipelines.stages.impl.lowcode

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.BusinessProcMgmtSys
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "update-bp-grouping", buildTool = ["any"], type = ProjectType.LIBRARY)
class UpdateBpGrouping {
    BuildContext context

    void run() {
        if (context.registryRegulations.filesToDeploy.get(RegulationType.BP_GROUPING)) {
            context.script.dir("${context.workDir}/${RegulationType.BP_GROUPING.value}") {
                try {
                    context.logger.info("Updating bp-grouping configmap")
                    String BP_GROUPING_CONFIG_FILE = "bp-grouping.yml"
                    String bpGroupingYaml = "bp-grouping:\\n" +
                            "${context.script.sh(script: """x=2; awk '{printf "%"'\$x'"s%s\\n", "", \$0}' \
                        ${BP_GROUPING_CONFIG_FILE}""", returnStdout: true).replaceAll("\n", "\\\\n").replaceAll("\"", "")}"
                    context.platform.patchConfigMapKey(BusinessProcMgmtSys.BP_GROUPING_CONFIG_MAP,
                            BP_GROUPING_CONFIG_FILE, bpGroupingYaml)
                    context.logger.info("bp-grouping configmap have been successfully updated")
                    context.platform.triggerDeploymentRollout(BusinessProcMgmtSys.USER_PROCESS_MANAGEMENT_DEPLOYMENT_NAME)
                } catch (any) {
                    context.logger.error("Error during uploading bp-grouping changes")
                    context.stageFactory.runStage(context.RESTORE_STAGE, context)
                }
            }
        }
    }
}
