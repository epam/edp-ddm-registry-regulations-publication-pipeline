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
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "update-theme-login-page", buildTool = ["any"], type = ProjectType.LIBRARY)
class UpdateThemeLoginPage {
    BuildContext context

    void run() {
        if (context.registryRegulations.deployStatus("update-theme-login-page",
                "${RegulationType.GLOBAL_VARS.value}")) {
            try {
                String GLOBAL_VARS_FILE = "${RegulationType.GLOBAL_VARS.value}/camunda-global-system-vars.yml"
                String themeFile = context.script.readYaml(file: GLOBAL_VARS_FILE)["themeFile"]
                if (themeFile) {
                    String idGovUaOfficerAuthFlow = "id-gov-ua-officer"
                    String authFlowYaml
                    ["officer", "citizen", idGovUaOfficerAuthFlow].each {
                        if (it == idGovUaOfficerAuthFlow && context.platform.checkObjectExists("keycloakauthflow", idGovUaOfficerAuthFlow)) {
                            authFlowYaml = context.platform.get("keycloakauthflows.v1.edp.epam.com", idGovUaOfficerAuthFlow, "-o yaml --ignore-not-found=true")
                        }
                        if (it == "officer" || it == "citizen") {
                            authFlowYaml = context.platform.get("keycloakauthflows.v1.edp.epam.com", "${it}-portal-dso-${it}-auth-flow", "-o yaml --ignore-not-found=true")
                        }
                        try {
                            String tmpFile = "tmp-${it}.yml"
                            context.script.writeFile(file: tmpFile, text: authFlowYaml)
                            context.script.sh("""sed -i 's/themeFile:.*/themeFile: ${themeFile}/' ${tmpFile}""")
                            context.platform.apply(tmpFile)
                            context.script.sh("rm -f ${tmpFile}")
                        } catch (any) {
                            context.logger.info("Failed to apply tmp-${it}.yml")
                        }
                    }
                } else {
                    context.logger.info("Theme file is not set, using default")
                }
            }
            catch (any) {
                context.logger.error("Failed to update theme file")
            }
            context.registryRegulations.getChangedStatusOrFiles("save", "update-theme-login-page",
                    "--file ${context.getWorkDir()}/${RegulationType.GLOBAL_VARS.value}")
        } else {
            context.logger.info("Skip update-theme-login-page due to no changes")
        }
    }
}
