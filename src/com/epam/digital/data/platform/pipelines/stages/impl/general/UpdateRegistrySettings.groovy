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

package com.epam.digital.data.platform.pipelines.stages.impl.general

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "update-registry-settings", buildTool = ["any"], type = ProjectType.LIBRARY)
class UpdateRegistrySettings {
    BuildContext context

    void run() {
        if (context.script.fileExists(context.registry.REGISTRY_SETTINGS_FILE_PATH)) {
            try {
                LinkedHashMap registrySettings = context.script.readYaml(file: context.registry.REGISTRY_SETTINGS_FILE_PATH)
                String title, titleFull
                if (registrySettings["settings"]["general"]["title"] && registrySettings["settings"]["general"]["titleFull"]) {
                    registrySettings["settings"]["general"]["title"] = registrySettings["settings"]["general"]["title"]
                            .replaceAll("'", "’").replaceAll("`", "’")
                    registrySettings["settings"]["general"]["titleFull"] = registrySettings["settings"]["general"]["titleFull"]
                            .replaceAll("'", "’").replaceAll("`", "’")
                    title = registrySettings["settings"]["general"]["title"]
                    titleFull = registrySettings["settings"]["general"]["titleFull"]
                } else {
                    title = ''
                    titleFull = ''
                }
                ["officer", "citizen"].each {
                    LinkedHashMap authFlowYaml = context.script.readYaml(text: context.platform
                            .get("keycloakauthflows.v1.edp.epam.com",
                                    "${it}-portal-dso-${it}-auth-flow", "-o yaml"))
                    authFlowYaml.spec.authenticationExecutions[1].authenticatorConfig.config.title = title
                    authFlowYaml.spec.authenticationExecutions[1].authenticatorConfig.config.titleFull = titleFull
                    String tmpFile = "tmp-${it}.yml"
                    context.script.writeYaml(file: tmpFile, data: authFlowYaml)
                    context.platform.apply(tmpFile)
                    context.script.sh("rm -f ${tmpFile}")
                }

                String asJson = context.script.writeJSON returnText: true, json: registrySettings
                String registrySettingsJson = "const REGISTRY_SETTINGS = ${asJson.replaceAll('"', '\'')};"
                if (context.platform.patchConfigMapKey("registry-settings-js", "registry-settings.js",
                        registrySettingsJson)) {
                    context.platform.triggerDeploymentRollout("citizen-portal,officer-portal")
                }
            }
            catch (Exception e) {
                context.logger.warn("Failed to update registry settings:\n ${e}")
            }
        }
    }

}
