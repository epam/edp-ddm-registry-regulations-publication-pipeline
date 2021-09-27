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
            ArrayList<String> filesToDeploy = context.registryRegulations.filesToDeploy.get(RegulationType.ROLES)
            if (filesToDeploy) {
                filesToDeploy.each { file ->
                    String roles = context.script.readFile(file: file)
                    String realmName = context.script.sh(script: "basename ${file} .yml", returnStdout: true).trim()
                    String KEYCLOAK_REALM_ROLE_BATCH_CR = "KeycloakRealmRoleBatch.v1.edp.epam.com"
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
                        LinkedHashMap<String, String> binding = ["realmName": realmName, "roles": roles]
                        String destination = "${realmName}-roles.yaml"
                        context.script.writeFile(file: destination, text: TemplateRenderer.renderTemplate(template, binding))
                        context.platform.apply(destination)
                        context.logger.info("Roles from ${file} have been sucessfully created")
                    }
                }
            } else {
                context.logger.info("Skip ${RegulationType.ROLES.value} creation due to empty change list")
            }
        } catch (any) {
            context.logger.error("Error during creating keycloak roles")
            context.stageFactory.runStage(context.RESTORE_STAGE, context)
        }
    }
}
