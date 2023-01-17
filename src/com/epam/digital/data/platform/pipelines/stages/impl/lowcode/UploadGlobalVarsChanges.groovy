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

@Stage(name = "upload-global-vars-changes", buildTool = ["any"], type = ProjectType.LIBRARY)
class UploadGlobalVarsChanges {
    BuildContext context

    void run() {
        context.script.dir("${context.workDir}/${RegulationType.GLOBAL_VARS.value}") {
            try {
                context.logger.info("Updating camunda global vars")
                String CAMUNDA_GLOBAL_VARS_FILE = "camunda-global-system-vars.yml"
                String camundaGlobalVarsYaml = "camunda:\\n  system-variables:\\n" +
                        "${context.script.sh(script: """x=4; awk '{gsub(/^[ \\t\\r\\n]+\$/, "", \$0); if ( \$0 ) printf "%"'\$x'"s%s_%s\\n", "", "const", \$0}' \
                        ${CAMUNDA_GLOBAL_VARS_FILE}""", returnStdout: true).replaceAll("\n", "\\\\n")}"
                context.bpmsConfigMapsChanged["globalVars"] = context.platform.patchConfigMapKey(BusinessProcMgmtSys.GLOBAL_VARS_CONFIG_MAP,
                        CAMUNDA_GLOBAL_VARS_FILE, camundaGlobalVarsYaml)
                context.logger.info("Camunda global have been successfully updated")

                context.logger.info("Updating registry env variables for portals")
                String asJson = context.script.sh(script: """x=2; awk '{gsub(/^[ \\t\\r\\n]+\$/, "", \$0); if ( \$0 ) printf "%"'\$x'"s%s%s\\n", "", \$0, ","}' \
                        ${CAMUNDA_GLOBAL_VARS_FILE}""", returnStdout: true)
                        .replaceAll("\n", "\\\\n")
                        .replaceAll(': ', ': \'')
                        .replaceAll(',', '\',')
                String jsRegistryEnvVarsJson = "const REGISTRY_ENVIRONMENT_VARIABLES = {\\n  ${asJson}};"
                if (context.platform.patchConfigMapKey("registry-environment-js", "registry-environment.js",
                        jsRegistryEnvVarsJson)) {
                    context.platform.triggerDeploymentRollout("citizen-portal,officer-portal")
                }
                context.logger.info("Registry env variables have been successfully updated")
            } catch (any) {
                context.logger.error("Error during uploading global variables changes")
                context.stageFactory.runStage(context.RESTORE_STAGE, context)
            }
        }
    }
}
