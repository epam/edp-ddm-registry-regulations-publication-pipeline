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
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.BusinessProcMgmtSys
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage
import com.epam.digital.data.platform.pipelines.tools.TemplateRenderer

@Stage(name = "delete-registry", buildTool = ["any"], type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class DeleteRegistry {
    BuildContext context

    private String CLEANUP_REGISTRY_SQL = "CleanupRegistry.sql"
    private String CLEANUP_PROCESS_HISTORY_SQL = "CleanupProcessHistory.sql"
    private String CLEANUP_REDASH_USERS_SQL = "CleanupRedashUsers.sql"
    private String CLEANUP_CAMUNDA_SQL = "CleanupCamunda.sql"
    private String CLEANUP_NOTIFICATIONS_SQL = "CleanupNotifications.sql"
    private String REDASH_VIEWER_SECRET = "redash-viewer-secret"
    private String REDASH_VIEWER_DB_NAME = "redash_viewer"
    private String REDASH_VIEWER_DB_USER = "redash_viewer_role"

    void run() {
        LinkedHashMap parallelDeletion = [:]
        ArrayList systemRoles = []
        String subconninfo

        parallelDeletion["cleanPostgresDB"] = {
            context.platform.scale("deployment/${BusinessProcMgmtSys.BPMS_DEPLOYMENT_NAME}", 0)
            context.platform.scale("deployment/${BusinessProcMgmtSys.BP_ADMIN_PORTAL_DEPLOYMENT_NAME}", 0)
            Map binding = ["OWNER_ROLE": context.postgres.ownerRole]

            context.logger.info("Copy DB scripts on postgres clusters")
            String template = context.script.libraryResource("sql/${CLEANUP_REGISTRY_SQL}")
            context.script.writeFile(file: "sql/${CLEANUP_REGISTRY_SQL}", text: TemplateRenderer.renderTemplate(template, binding))
            String cleanupProcessHistorySqlResource = context.script.libraryResource("sql/${CLEANUP_PROCESS_HISTORY_SQL}")
            context.script.writeFile(file: "sql/${CLEANUP_PROCESS_HISTORY_SQL}", text: cleanupProcessHistorySqlResource)
            String cleanupCamundaSqlResource = context.script.libraryResource("sql/${CLEANUP_CAMUNDA_SQL}")
            context.script.writeFile(file: "sql/${CLEANUP_CAMUNDA_SQL}", text: cleanupCamundaSqlResource)
            String cleanupNotificationsSqlResource = context.script.libraryResource("sql/${CLEANUP_NOTIFICATIONS_SQL}")
            context.script.writeFile(file: "sql/${CLEANUP_NOTIFICATIONS_SQL}", text: cleanupNotificationsSqlResource)
            context.script.sh(script: "oc rsync --no-perms=true sql/ ${context.postgres.masterRepPod}:/tmp/")
            context.script.sh(script: "oc rsync --no-perms=true sql/ ${context.postgres.masterPod}:/tmp/")

            context.logger.info("Check the subscription")
            Boolean subexist = context.postgres.psqlCommand(context.postgres.masterRepPod,
                    "select 1 from pg_subscription where subname = 'operational_sub';",
                    context.registry.name, context.postgres.analytical_pg_user).trim().toBoolean()

            if (subexist) {
                context.logger.info("Get the subscription connection settings")
                subconninfo = context.postgres.psqlCommand(context.postgres.masterRepPod,
                        "select subconninfo from pg_subscription where subname = 'operational_sub';",
                        context.registry.name, context.postgres.analytical_pg_user).trim()

                context.logger.info("Deleting the analytical subscription")
                context.postgres.psqlCommand(context.postgres.masterRepPod, "DROP SUBSCRIPTION operational_sub;",
                        context.registry.name, context.postgres.analytical_pg_user)
            } else {
                context.logger.info("Subscription doesn't exist. Configuring data for subscription creation.")
                String getPassword = context.platform.getSecretValue("operational-pguser-postgres", "password")
                String getPort = context.platform.getSecretValue("operational-pguser-postgres", "port")
                String getUser = context.platform.getSecretValue("operational-pguser-postgres", "user")
                subconninfo = "dbname=registry host=operational-primary user=${getUser} password=${getPassword} port=${getPort}"
            }
            
            context.logger.info("Cleaning registry DB on analytical cluster")
            context.postgres.psqlScript(context.postgres.masterRepPod, "/tmp/${CLEANUP_REGISTRY_SQL}", context.postgres.analytical_pg_user, "-d ${context.registry.name}")

            context.logger.info("Cleaning registry DB on operational cluster")
            context.postgres.psqlScript(context.postgres.masterPod, "/tmp/${CLEANUP_REGISTRY_SQL}", context.postgres.operational_pg_user, "-d ${context.registry.name}")

            context.logger.info("Recreating a subscription on an analytical instance")
            context.postgres.psqlCommand(context.postgres.masterRepPod,
                    "CREATE SUBSCRIPTION operational_sub CONNECTION '${subconninfo}' PUBLICATION analytical_pub WITH(create_slot=false,slot_name=operational_sub);",
                    context.registry.name, context.postgres.analytical_pg_user)

            context.logger.info("Cleaning process_history DB on operational cluster")
            context.postgres.psqlScript(context.postgres.masterPod, "/tmp/${CLEANUP_PROCESS_HISTORY_SQL}", context.postgres.operational_pg_user)

            context.logger.info("Cleaning camunda DB on operational cluster")
            context.postgres.psqlScript(context.postgres.masterPod, "/tmp/${CLEANUP_CAMUNDA_SQL}", context.postgres.operational_pg_user)

            context.logger.info("Cleaning notifications DB on operational cluster")
            context.postgres.psqlScript(context.postgres.masterPod, "/tmp/${CLEANUP_NOTIFICATIONS_SQL}", context.postgres.operational_pg_user)

            context.platform.scale("deployment/${BusinessProcMgmtSys.BPMS_DEPLOYMENT_NAME}", 1)
            context.platform.scale("deployment/${BusinessProcMgmtSys.BP_ADMIN_PORTAL_DEPLOYMENT_NAME}", 1)

            context.logger.info("Removing analytics roles from registry db on analytical cluster")
            LinkedHashMap registryPostgresValues = context.script.readYaml text: context.script.sh(script: """helm get values registry-postgres -a -n ${context.namespace}""", returnStdout: true)
            registryPostgresValues["postgresCluster"]["secrets"]["citusRolesSecrets"].each { role ->
                if (role.value.contains("analytics_"))
                    systemRoles += role.value
            }
            List rolesToDelete = context.postgres.psqlCommand(context.postgres.masterRepPod,
                    "SELECT rolname FROM pg_roles WHERE rolname LIKE 'analytics_%' AND rolname not in ('${systemRoles.join("','")}');", context.registry.name, context.postgres.analytical_pg_user).replaceAll(" ","").tokenize('\n')
            if (rolesToDelete) {
                rolesToDelete.each { roleName ->
                    try {
                        context.postgres.psqlCommand(context.postgres.masterRepPod,
                                "call p_delete_analytics_user('${roleName}')", context.registry.name, context.postgres.analytical_pg_user)
                    } catch (any) {
                        if (context.postgres.psqlCommand(context.postgres.masterRepPod,
                                "SELECT 1 FROM pg_roles WHERE rolname='${roleName}';",
                                context.registry.name, context.postgres.analytical_pg_user).trim() == '1') {
                            context.script.error("Removing of analytic role ${roleName} from database $context.registry.name FAILED")
                        }
                    }
                }
            }
        }
        parallelDeletion["removeRedashResources"] = {
            context.logger.info("Removing Redash resources")
            String cleanupRedashUsers = context.script.libraryResource("sql/${CLEANUP_REDASH_USERS_SQL}")
            context.script.writeFile(file: "sql_redash/${CLEANUP_REDASH_USERS_SQL}", text: cleanupRedashUsers)
            context.script.sh(script: "oc rsync --no-perms=true sql_redash/ ${context.postgres.masterRepPod}:/tmp/")
            context.platform.podExec(context.postgres.masterRepPod,
                    "bash -c \'export PGPASSWORD=${context.platform.getSecretValue(REDASH_VIEWER_SECRET, "postgresqlPassword")}; psql ${REDASH_VIEWER_DB_NAME} -U ${REDASH_VIEWER_DB_USER} -hlocalhost -f tmp/${CLEANUP_REDASH_USERS_SQL}\'", "")
            context.redash.deleteRedashResource("${context.redash.viewerUrl}/api/dashboards",
                    context.redash.viewerApiKey)
            context.redash.deleteRedashResource("${context.redash.viewerUrl}/api/data_sources",
                    context.redash.viewerApiKey)
            context.redash.deleteRedashResource("${context.redash.viewerUrl}/api/groups", context.redash.viewerApiKey)
            context.logger.info("Remove audit dashboards job")
            try {
                context.script.sh(script: "oc delete job create-dashboard-job -n $context.namespace ")
            } catch (any) {
                context.logger.info("WARN: create-dashboard-job was not removed")
            }
        }
        context.script.parallel(parallelDeletion)
    }
}
