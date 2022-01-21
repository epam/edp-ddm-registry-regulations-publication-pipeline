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

package com.epam.digital.data.platform.pipelines.stages.impl.deleteRelease

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.BusinessProcMgmtSys
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.FormManagement
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage
import com.epam.digital.data.platform.pipelines.tools.TemplateRenderer
import com.epam.digital.data.platform.pipelines.tools.Helm

@Stage(name = "delete-registry", buildTool = ["any"], type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class DeleteRegistry {
    BuildContext context

    private String CLEANUP_REGISTRY_SQL = "CleanupRegistry.sql"
    private String CLEANUP_PROCESS_HISTORY_SQL = "CleanupProcessHistory.sql"
    private final String REGISTRY_CONF_CHART_NAME = "registry-configuration"
    private final String DEPLOY_TEMPLATES_PATH = "deploy-templates"
    private final String REGISTRY_CONF_CONFIGMAP = "registry-pipeline-stage-name"

    public final static String USER_SETTINGS_SERVICE_PERSISTENCE_DEPLOYMENT_NAME = "user-settings-service-persistence-deployment"

    void run() {
        try {
            context.logger.info("Cleaning form provider DB")
            context.platform.scale("deployment/${FormManagement.PROVIDER_DEPLOYMENT_NAME}", 0)
            context.platform.podExec(FormManagement.PROVIDER_DB_POD, "mongo ${FormManagement.PROVIDER_DB_NAME} " +
                    "--authenticationDatabase ${FormManagement.AUTH_DATABASE} " +
                    "-u ${context.platform.getSecretValue(FormManagement.PROVIDER_DB_SECRET, "username")} " +
                    "-p ${context.platform.getSecretValue(FormManagement.PROVIDER_DB_SECRET, "password")} " +
                    "--eval \"db.dropDatabase()\"", FormManagement.PROVIDER_DB_CONTAINER)
            context.platform.scale("deployment/${FormManagement.PROVIDER_DEPLOYMENT_NAME}", 1)

            context.logger.info("Cleaning form modeler DB")
            context.platform.scale("deployment/${FormManagement.MODELER_DEPLOYMENT_NAME}", 0)
            context.platform.podExec(FormManagement.MODELER_DB_POD, "mongo ${FormManagement.MODELER_DB_NAME} " +
                    "--authenticationDatabase ${FormManagement.AUTH_DATABASE} " +
                    "-u ${context.platform.getSecretValue(FormManagement.MODELER_DB_SECRET, "username")} " +
                    "-p ${context.platform.getSecretValue(FormManagement.MODELER_DB_SECRET, "password")} " +
                    "--eval \"db.dropDatabase()\"", FormManagement.MODELER_DB_CONTAINER)
            context.platform.scale("deployment/${FormManagement.MODELER_DEPLOYMENT_NAME}", 1)
        } catch (any) {
            context.logger.warn("There was an error during form management databases cleanup")
        }

        context.platform.scale("deployment/${BusinessProcMgmtSys.BPMS_DEPLOYMENT_NAME}", 0)
        context.platform.scale("deployment/${BusinessProcMgmtSys.BP_ADMIN_PORTAL_DEPLOYMENT_NAME}", 0)
        Map binding = ["OWNER_ROLE": context.citus.ownerRole]

        context.logger.info("Copy DB scripts on citus master and replica")
        String template = context.script.libraryResource("sql/${CLEANUP_REGISTRY_SQL}")
        context.script.writeFile(file: "sql/${CLEANUP_REGISTRY_SQL}", text: TemplateRenderer.renderTemplate(template, binding))
        String cleanupProcessHistorySqlResource = context.script.libraryResource("sql/${CLEANUP_PROCESS_HISTORY_SQL}")
        context.script.writeFile(file: "sql/${CLEANUP_PROCESS_HISTORY_SQL}", text: cleanupProcessHistorySqlResource)
        context.script.sh(script: "oc rsync sql/ ${context.citus.masterRepPod}:/tmp/")
        context.script.sh(script: "oc rsync sql/ ${context.citus.masterPod}:/tmp/")

        context.logger.info("Cleaning registry DB on replica")
        context.citus.psqlScript(context.citus.masterRepPod, "/tmp/${CLEANUP_REGISTRY_SQL}", "-d ${context.registry.name}")

        context.logger.info("Cleaning registry DB on master")
        context.citus.psqlScript(context.citus.masterPod, "/tmp/${CLEANUP_REGISTRY_SQL}", "-d ${context.registry.name}")

        context.logger.info("Cleaning process_history DB on master")
        context.citus.psqlScript(context.citus.masterPod, "/tmp/${CLEANUP_PROCESS_HISTORY_SQL}")

        context.platform.scale("deployment/${BusinessProcMgmtSys.BPMS_DEPLOYMENT_NAME}", 1)
        context.platform.scale("deployment/${BusinessProcMgmtSys.BP_ADMIN_PORTAL_DEPLOYMENT_NAME}", 1)

        context.logger.info("Removing Redash objects")
        LinkedHashMap officerUsersYaml = context.script.readYaml file: "roles/officer.yml"
        officerUsersYaml["roles"].each { String role ->
            try {
                context.citus.psqlCommand(context.citus.masterRepPod,
                        "call p_delete_analytics_user('analytics_${role["name"]}')", context.registry.name)
            }
            catch (any) {
                context.logger.info("Removing of Redash roles failed. Possibly, there are no Redash roles in database.")
            }
        }
        context.redash.deleteRedashResource("${context.redash.viewerUrl}/api/data_sources",
                context.redash.viewerApiKey)
        context.redash.deleteRedashResource("${context.redash.viewerUrl}/api/groups", context.redash.viewerApiKey)

        context.logger.info("Removing keycloak resources")
        String centralGerritUrl = context.platform.getJsonPathValue("configmap", REGISTRY_CONF_CONFIGMAP,
                ".data.gerritCentralUrl")
        String registryConfRepoPath = context.dnsWildcard.startsWith("apps.cicd") ? 'mdtu-ddm/general' : 'components/registry'
        String registryConfRepoUrl = "${centralGerritUrl}/${registryConfRepoPath}/$REGISTRY_CONF_CHART_NAME"

        context.gitClient.checkout(registryConfRepoUrl,
                "main", "edp-gerrit-ciuser")
        ["keycloakrealmidentityproviders", "keycloakauthflows", "keycloakclients", "keycloakclientscopes",
         "keycloakrealmgroups", "keycloakrealmrolebatches",
         "keycloakrealmroles", "keycloakrealms"].each { resourceType ->
            ArrayList<String> resourcesList = context.platform.getAll(resourceType, "--no-headers " +
                    "-o=custom-columns=NAME:.metadata.name -l created-by=$REGISTRY_CONF_CHART_NAME").tokenize()
            resourcesList.each { resource ->
                if (!resource.matches("(.*)admin(.*)") || resource.matches("bpms(.*)")) {
                    context.platform.deleteObject(resourceType, resource)
                }
            }
            if (resourceType == "keycloakrealmroles") {
                ArrayList<String> registryRolesList = context.platform.getAll("keycloakrealmrole", "--no-headers " +
                        "-o=custom-columns=NAME:.metadata.name").tokenize()
                registryRolesList.each { registryRole ->
                    if (registryRole.matches("citizen(.*)") || registryRole.matches("officer(.*)")) {
                        context.platform.deleteObject("keycloakrealmrole", registryRole)
                    }
                }
            }
        }
        context.logger.info("Removing keycloakclient in user-management namespace")
        context.platform.deleteObject("keycloakclients", "$context.namespace-citizen-portal", "-n user-management")
        Helm.upgrade(context, REGISTRY_CONF_CHART_NAME, DEPLOY_TEMPLATES_PATH,
                ['': ''], "-f ${context.registryRegulations.getRegistryConfValues()}",
                context.namespace, true)
        context.logger.info("Restart user-settings-persistence")
        context.platform.scale("deployment/$USER_SETTINGS_SERVICE_PERSISTENCE_DEPLOYMENT_NAME", 0)
        context.platform.scale("deployment/$USER_SETTINGS_SERVICE_PERSISTENCE_DEPLOYMENT_NAME", 1)
    }
}
