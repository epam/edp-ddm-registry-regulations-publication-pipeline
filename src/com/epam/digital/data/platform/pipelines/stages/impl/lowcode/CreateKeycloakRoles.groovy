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
import com.epam.digital.data.platform.pipelines.tools.TemplateRenderer

@Stage(name = "create-keycloak-roles", buildTool = ["any"], type = ProjectType.LIBRARY)
class CreateKeycloakRoles {
    BuildContext context

    void run() {
        try {
            ArrayList<String> filesToDeploy
            if (context.getParameterValue("FULL_DEPLOY", "false").toBoolean()) {
                filesToDeploy = context.registryRegulations.getAllRegulations(RegulationType.ROLES).join(",").tokenize(',')
                context.logger.info("filesToDeploy: ${filesToDeploy}")
            } else {
                filesToDeploy = context.registryRegulations.getChangedStatusOrFiles("plan", "create-keycloak-roles",
                        "--file-detailed ${context.getWorkDir()}/${RegulationType.ROLES.value}")
            }

            if (filesToDeploy) {
                filesToDeploy.each { file ->
                    def portalNames = ["officer", "citizen"]
                    if (!file.contains(".gitkeep")) {
                        String roles = context.script.readFile(file: file.trim())
                        String realmName = context.script.sh(script: "basename ${file} .yml", returnStdout: true).trim()
                        String KEYCLOAK_REALM_ROLE_BATCH_CR = "KeycloakRealmRoleBatch"
                        if (roles.isEmpty()) {
                            if (context.platform.checkObjectExists(KEYCLOAK_REALM_ROLE_BATCH_CR, realmName)) {
                                context.platform.deleteObject(KEYCLOAK_REALM_ROLE_BATCH_CR, realmName)
                                context.logger.info("KeycloakRealmRoleBatch ${realmName} was deleted")
                            } else {
                                context.logger.info("Skip ${realmName} role batch creation due to empty ${file} file")
                            }
                        } else {
                            context.logger.info("Creating roles from ${file}")
                            String template = context.script.libraryResource("${context.YAML_RESOURCES_RELATIVE_PATH}" +
                                    "/keycloak/keycloak-realm-roles-batch.yaml")
                            String realm = realmName
                            if (portalNames.contains(realm)) {
                                realm += "-portal"
                            }
                            LinkedHashMap<String, String> binding = ["realmName": realmName, "roles": roles, "realm": realm]
                            String destination = "${realmName}-roles.yaml"
                            context.script.writeFile(file: destination, text: TemplateRenderer.renderTemplate(template, binding))
                            context.platform.apply(destination)
                            context.logger.info("Roles from ${file} have been sucessfully created")
                        }
                    }
                }
                context.script.dir("${context.workDir}/${RegulationType.ROLES.value}") {
                    try {
                        ["officer", "citizen", "external-system"].each {
                            context.logger.info("Updating ${it}-roles configmap")
                            String rolesConfigFile = "${it}.yml"
                            String rolesConfigmapKey = "${it}-roles.yml"
                            String configmapName = "${it}-roles"
                            String rolesConfigmapYaml = "registry-regulation:\\n  ${it}:\\n" +
                                    "${context.script.sh(script: """x=4; awk '{printf "%"'\$x'"s%s\\n", "", \$0}' \
                        ${rolesConfigFile}""", returnStdout: true).replaceAll("\n", "\\\\n")}"
                            context.bpmsConfigMapsChanged["${it}Roles"] = context.platform.patchConfigMapKey(configmapName,
                                    rolesConfigmapKey, rolesConfigmapYaml)
                            context.logger.info("Configmap ${it}-roles have been successfully updated")
                        }
                    } catch (any) {
                        context.logger.error("Error during officer-roles/citizen-roles configmap updating")
                    }
                }
                context.registryRegulations.getChangedStatusOrFiles("save", "create-keycloak-roles",
                        "--file-detailed ${context.getWorkDir()}/${RegulationType.ROLES.value}")
            } else {
                context.logger.info("Skip ${RegulationType.ROLES.value} creation due to empty change list")
            }
        } catch (any) {
            context.logger.error("Error during creating keycloak roles")
            context.stageFactory.runStage(context.RESTORE_STAGE, context)
        }
    }
}
