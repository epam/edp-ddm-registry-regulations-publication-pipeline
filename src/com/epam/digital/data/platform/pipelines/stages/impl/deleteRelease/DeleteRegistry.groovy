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

package com.epam.digital.data.platform.pipelines.stages.impl.deleteRelease

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.helper.DecodeHelper
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.BusinessProcMgmtSys
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.FormManagement
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage
import com.epam.digital.data.platform.pipelines.tools.TemplateRenderer

@Stage(name = "delete-registry", buildTool = ["any"], type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class DeleteRegistry {
    BuildContext context

    private String CLEANUP_REGISTRY_SQL = "CleanupRegistry.sql"
    private String CLEANUP_PROCESS_HISTORY_SQL = "CleanupProcessHistory.sql"
    private String CLEANUP_REDASH_USERS_SQL = "CleanupRedashUsers.sql"
    private String REDASH_POD_NAME = "redash-viewer-postgresql-0"

    LinkedHashMap formProviderSecretJson
    LinkedHashMap formModelerSecretJson

    void run() {

        formProviderSecretJson = context.platform.getAsJson("secret", FormManagement.PROVIDER_DB_SECRET)["data"]
        formModelerSecretJson = context.platform.getAsJson("secret", FormManagement.MODELER_DB_SECRET)["data"]

        LinkedHashMap parallelDeletion = [:]
        parallelDeletion["cleanFormManagementDB"] = {
            try {
                context.logger.info("Cleaning form provider DB")
                cleanFormManagementDB(FormManagement.PROVIDER_DEPLOYMENT_NAME, FormManagement.PROVIDER_DB_POD,
                        FormManagement.PROVIDER_DB_NAME, DecodeHelper.decodeBase64(formProviderSecretJson["user"]),
                        DecodeHelper.decodeBase64(formProviderSecretJson["password"]),
                        FormManagement.PROVIDER_DB_CONTAINER)
            } catch (any) {
                context.logger.warn("There was an error during form management databases cleanup")
            }
        }
        parallelDeletion["cleanCitusDB"] = {
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

            context.logger.info("Removing analytics roles from db")
            LinkedHashMap officerUsersYaml = context.script.readYaml file: "roles/officer.yml"
            officerUsersYaml["roles"].each { String role ->
                try {
                    context.citus.psqlCommand(context.citus.masterRepPod,
                            "call p_delete_analytics_user('analytics_${role["name"]}')", context.registry.name)
                } catch (any) {
                    if (context.citus.psqlCommand(context.citus.masterRepPod,
                            "SELECT 1 FROM pg_roles WHERE rolname='analytics_${role["name"]}';",
                            context.registry.name).trim() == '1') {
                        context.script.error("Removing of analytic role $role from database $context.registry.name FAILED")
                    }
                }
            }
        }
        parallelDeletion["removeRedashResources"] = {
            context.logger.info("Removing Redash resources")
            String cleanupRedashUsers = context.script.libraryResource("sql/${CLEANUP_REDASH_USERS_SQL}")
            context.script.writeFile(file: "sql_redash/${CLEANUP_REDASH_USERS_SQL}", text: cleanupRedashUsers)
            context.script.sh(script: "oc rsync sql_redash/ ${REDASH_POD_NAME}:/tmp/")
            context.platform.podExec(REDASH_POD_NAME,
                    "bash -c \'export PGPASSWORD=${context.platform.getSecretValue("redash-chart-postgresql", "postgresql-password")}; psql redash -U redash -f tmp/${CLEANUP_REDASH_USERS_SQL}\'", "")
            context.redash.deleteRedashResource("${context.redash.viewerUrl}/api/data_sources",
                    context.redash.viewerApiKey)
            context.redash.deleteRedashResource("${context.redash.viewerUrl}/api/groups", context.redash.viewerApiKey)
        }
        context.script.parallel(parallelDeletion)
    }

    void cleanFormManagementDB(String deploymentName, String dbPodName, String dbName,
                               String dbUser, String dbPass, String dbContainerName) {
        context.platform.scale("deployment/$deploymentName", 0)
        context.platform.podExec(dbPodName, "mongo $dbName " +
                "--authenticationDatabase ${FormManagement.AUTH_DATABASE} " +
                "-u $dbUser " +
                "-p $dbPass " +
                "--eval \"db.dropDatabase()\"", dbContainerName)
        context.platform.scale("deployment/$deploymentName", 1)
    }
}
