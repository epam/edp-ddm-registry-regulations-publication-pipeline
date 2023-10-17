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

package com.epam.digital.data.platform.pipelines.stages.impl.lowcode

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "update-registry-logos", buildTool = ["any"], type = ProjectType.LIBRARY)
class UpdateRegistryLogos {
    BuildContext context

    void run() {
        def stageName = "update-registry-logos"
        def configmapName = "registry-logos"
        def changedAssetsFiles
        if (context.getParameterValue("FULL_DEPLOY", "false").toBoolean()) {
            changedAssetsFiles = context.registryRegulations.getAllRegulations(RegulationType.ASSETS).join(",").tokenize(',')
        } else {
            changedAssetsFiles = context.registryRegulations.getChangedStatusOrFiles("plan", stageName,
                    "--file-detailed ${context.getWorkDir()}/${RegulationType.ASSETS.value}")
        }
        if (changedAssetsFiles) {
            try {
                def logoProperties = [
                        "header-logo.svg": "logoHeader",
                        "loader-logo.svg": "logoLoader",
                        "favicon.png"    : "logoFavicon"
                ]
                def authenticatorConfigProperties = [:]
                def configMapProperties = [:]
                changedAssetsFiles.each { logoFile ->
                    String logoFileName = context.script.sh(script: "basename ${logoFile}", returnStdout: true).trim()
                    if (logoProperties.containsKey(logoFileName)) {
                        String logoFileBase64 = context.script.sh(script: """base64 -w 0 ${logoFile}""", returnStdout: true).trim()
                        authenticatorConfigProperties.put(logoProperties.get(logoFileName), logoFileBase64)
                        configMapProperties.put(logoFileName, logoFileBase64)
                    }
                }
                if (configMapProperties.isEmpty()) {
                    context.logger.info("Logo files have not been changed")
                } else {
                    LinkedHashMap registryLogosYaml = context.script.readYaml(text: context.platform.get("cm",
                            "${configmapName}", "-o yaml"))
                    registryLogosYaml.binaryData.putAll(configMapProperties)
                    LinkedHashMap updatedRegistryLogosYaml = context.platform.removeYamlMetadata(registryLogosYaml)
                    String tmpFile = "tmp-${configmapName}.yml"
                    context.script.writeYaml(file: tmpFile, data: updatedRegistryLogosYaml)
                    context.platform.apply(tmpFile)
                    context.script.sh("rm -f ${tmpFile}")

                    context.keycloak.updateAuthenticatorConfigProperties(authenticatorConfigProperties)
                }
                context.registryRegulations.getChangedStatusOrFiles("save", stageName,
                        "--file-detailed ${context.getWorkDir()}/${RegulationType.ASSETS.value}")
            } catch (any) {
                context.script.error("Error during registry logos updating")
            }
        } else {
            context.logger.info("Skip update-registry-logos due to empty change list")
        }
    }
}
