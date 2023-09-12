/*
 * Copyright 2023 EPAM Systems.
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

    private final String LOWCODE_VALIDATOR_JAR = "/home/jenkins/registry-regulations-cli/registry-regulations-cli.jar"

    void run() {
        context.logger.info("Registry regulations files validation")
        String validatorParams = "" +
                "validate " +
                "--bp-auth-files=${context.registryRegulations.getAllRegulations(RegulationType.BUSINESS_PROCESS_AUTH).join(",")} " +
                "--bp-trembita-files=${RegulationType.BUSINESS_PROCESS_TREMBITA.getValue()}/${BpTrembitaFileType.EXTERNAL_SYSTEM.getValue()} " +
                "--bp-trembita-config=${RegulationType.BUSINESS_PROCESS_TREMBITA.getValue()}/${BpTrembitaFileType.CONFIG.getValue()} " +
                "--bpmn-files=${context.registryRegulations.getAllRegulations(RegulationType.BUSINESS_PROCESS).join(",")} " +
                "--dmn-files=${context.registryRegulations.getAllRegulations(RegulationType.BUSINESS_RULE).join(",")} " +
                "--form-files=${context.registryRegulations.getAllRegulations(RegulationType.UI_FORM).join(",")} " +
                "--global-vars-files=${context.registryRegulations.getAllRegulations(RegulationType.GLOBAL_VARS).join(",")} " +
                "--roles-files=${context.registryRegulations.getAllRegulations(RegulationType.ROLES).join(",")} " +
                "--liquibase-files=data-model/main-liquibase.xml " +
                "--datafactory-settings-files=${context.registry.SETTINGS_FILE} " +
                "--email-notification-template-folder=notifications/email " +
                "--inbox-notification-template-folder=notifications/inbox " +
                "--diia-notification-template-folder=notifications/diia " +
                "--excerpt-folders=excerpts,excerpts-docx,excerpts-csv " +
                "--bp-grouping-files=bp-grouping/bp-grouping.yml " +
                "--mock-integration-files=${context.registryRegulations.getAllRegulations(RegulationType.MOCK_INTEGRATIONS).join(",")} " +
                "--reports-files=${context.registryRegulations.getAllRegulations(RegulationType.REPORTS).join(",")} "

        if (context.script.fileExists(context.registry.REGISTRY_SETTINGS_FILE_PATH)) {
            validatorParams += "--registry-settings-files=${context.registry.REGISTRY_SETTINGS_FILE_PATH} "
        }

        try {
            context.script.sh(script: "java -jar -DOPENSHIFT_NAMESPACE=${context.namespace} ${LOWCODE_VALIDATOR_JAR} ${validatorParams} " +
                    "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
        }
        catch (any) {
            context.script.unstable("[JENKINS][WARNING] Registry regulations files did not pass validation")
            context.script.currentBuild.setResult('UNSTABLE')
        }
        context.logger.info("Registry regulations files have been successfully validated")
    }
}

