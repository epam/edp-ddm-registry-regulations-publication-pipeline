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

@Stage(name = "update-login-page", buildTool = ["any"], type = ProjectType.LIBRARY)
class UpdateLoginPage {
    BuildContext context

    void run() {
        if (context.registryRegulations.deployStatus("update-login-page",
                "${RegulationType.GLOBAL_VARS.value}")) {
            try {
                String GLOBAL_VARS_FILE = "${RegulationType.GLOBAL_VARS.value}/camunda-global-system-vars.yml"
                def yaml = context.script.readYaml(file: GLOBAL_VARS_FILE)
                String themeFile = yaml["themeFile"]
                String supportChannelUrl = yaml["supportChannelUrl"].replaceAll("&", "\\\\&")
                if (themeFile || supportChannelUrl) {
                    String idGovUaOfficerAuthFlow = "id-gov-ua-officer"
                    LinkedHashMap authFlowYaml
                    ["officer", "citizen", idGovUaOfficerAuthFlow].each {
                        if (it == idGovUaOfficerAuthFlow && context.platform.checkObjectExists("keycloakauthflow", idGovUaOfficerAuthFlow)) {
                            authFlowYaml = context.script.readYaml(text: context.platform.get("keycloakauthflows", idGovUaOfficerAuthFlow, "-o yaml --ignore-not-found=true"))
                        }
                        if (it == "officer" || it == "citizen") {
                            authFlowYaml = context.script.readYaml(text: context.platform.get("keycloakauthflows", "${it}-portal-dso-${it}-auth-flow", "-o yaml --ignore-not-found=true"))
                        }
                        try {
                            LinkedHashMap updatedAuthFlowYaml = context.platform.removeYamlMetadata(authFlowYaml)
                            String tmpFile = "tmp-${it}.yml"
                            context.script.writeYaml(file: tmpFile, data: updatedAuthFlowYaml)
                            if(themeFile) {
                                context.script.sh("""sed -i 's/themeFile:.*/themeFile: ${themeFile}/' ${tmpFile}""")
                            }
                            if(supportChannelUrl) {
                                context.script.sh("""sed -i 's|supportChannelUrl:.*|supportChannelUrl: ${supportChannelUrl}|' ${tmpFile}""")
                            } else {
                                context.script.sh("""sed -i 's|supportChannelUrl:.*|supportChannelUrl: ""|' ${tmpFile}""")
                            }
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
            context.registryRegulations.getChangedStatusOrFiles("save", "update-login-page",
                    "--file ${context.getWorkDir()}/${RegulationType.GLOBAL_VARS.value}")
        } else {
            context.logger.info("Skip update-login-page due to no changes")
        }
    }
}
