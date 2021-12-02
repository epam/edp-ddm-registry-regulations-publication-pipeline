/*
 * Copyright 2021 EPAM Systems.
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

package com.epam.digital.data.platform.pipelines.stages.impl.general

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.BpTrembitaFileType
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "registry-regulations-validation", buildTool = ["any"], type = [ProjectType.LIBRARY])
class RegistryRegulationsValidation {
    BuildContext context

    private final String LOWCODE_VALIDATOR_JAR = "/home/jenkins/registry-regulations-validator-cli/registry-regulations-validator-cli.jar"
    private final String DATA_VALIDATOR_JAR = "/home/jenkins/registry-regulations-validator/registry-regulations-validator.jar"

    void run() {
        context.logger.info("Registry regulations files validation")
        try {
            context.script.sh(script: "java -jar ${DATA_VALIDATOR_JAR} ${context.logLevel == "DEBUG" ? "1>&2" : ""}")
            context.script.sh(script: "java -jar ${LOWCODE_VALIDATOR_JAR} " +
                    "--bp-auth-files=${context.registryRegulations.filesToDeploy.get(RegulationType.BUSINESS_PROCESS_AUTH).join(",")} " +
                    "--bp-trembita-files=${getBpTrembitaExternalSystem()} " +
                    "--bp-trembita-config=${getBpTrembitaConfiguration()} " +
                    "--bpmn-files=${context.registryRegulations.filesToDeploy.get(RegulationType.BUSINESS_PROCESS).join(",")} " +
                    "--dmn-files=${context.registryRegulations.filesToDeploy.get(RegulationType.BUSINESS_RULE).join(",")} " +
                    "--form-files=${context.registryRegulations.filesToDeploy.get(RegulationType.UI_FORM).join(",")} " +
                    "--global-vars-files=${context.registryRegulations.filesToDeploy.get(RegulationType.GLOBAL_VARS).join(",")} " +
                    "--roles-files=${context.registryRegulations.filesToDeploy.get(RegulationType.ROLES).join(",")} " +
                    "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
        }
        catch (any) {
            context.script.error("Registry regulations files did not pass validation")
        }
        context.logger.info("Registry regulations files have been successfully validated")
    }

    String getBpTrembitaConfiguration() {
        return context.registryRegulations.filesToDeploy.get(RegulationType.BUSINESS_PROCESS_TREMBITA)
                .find { it.contains(BpTrembitaFileType.CONFIG.getValue()) }
    }

    String getBpTrembitaExternalSystem() {
        return context.registryRegulations.filesToDeploy.get(RegulationType.BUSINESS_PROCESS_TREMBITA)
                .find { it.contains(BpTrembitaFileType.EXTERNAL_SYSTEM.getValue()) }
    }
}
