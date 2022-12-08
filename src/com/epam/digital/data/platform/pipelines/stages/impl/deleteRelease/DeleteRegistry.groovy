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
    private String REDASH_POD_NAME = "redash-viewer-postgresql-0"

    void run() {
        LinkedHashMap parallelDeletion = [:]

        parallelDeletion["cleanPostgresDB"] = {
            context.platform.scale("deployment/${BusinessProcMgmtSys.BPMS_DEPLOYMENT_NAME}", 0)
            context.platform.scale("deployment/${BusinessProcMgmtSys.BP_ADMIN_PORTAL_DEPLOYMENT_NAME}", 0)
            Map binding = ["OWNER_ROLE": context.postgres.ownerRole]

            context.logger.info("Copy DB scripts on postgres clusters")
            String template = context.script.libraryResource("sql/${CLEANUP_REGISTRY_SQL}")
            context.script.writeFile(file: "sql/${CLEANUP_REGISTRY_SQL}", text: TemplateRenderer.renderTemplate(template, binding))
            String cleanupProcessHistorySqlResource = context.script.libraryResource("sql/${CLEANUP_PROCESS_HISTORY_SQL}")
            context.script.writeFile(file: "sql/${CLEANUP_PROCESS_HISTORY_SQL}", text: cleanupProcessHistorySqlResource)
            context.script.sh(script: "oc rsync --no-perms=true sql/ ${context.postgres.masterRepPod}:/tmp/")
            context.script.sh(script: "oc rsync --no-perms=true sql/ ${context.postgres.masterPod}:/tmp/")

            context.logger.info("Cleaning registry DB on analytical cluster")
            String srsubState = context.postgres.psqlCommand(context.postgres.masterRepPod,
                    "select count(*)from pg_subscription_rel where srsubstate <> 'r';",
                    context.registry.name, context.postgres.analytical_pg_user).trim()
            if (srsubState != '0') {
                context.postgres.psqlCommand(context.postgres.masterRepPod,
                        "alter subscription operational_sub refresh publication;",
                        context.registry.name, context.postgres.analytical_pg_user)
                context.postgres.psqlCommand(context.postgres.masterRepPod,
                                "alter subscription operational_sub enable",
                        context.registry.name, context.postgres.analytical_pg_user)
            }
            String isSubenabled = context.postgres.psqlCommand(context.postgres.masterRepPod,
                    "select subenabled from pg_subscription;",
                    context.registry.name, context.postgres.analytical_pg_user).trim()
            if (isSubenabled == 't') {
                context.postgres.psqlScript(context.postgres.masterRepPod, "/tmp/${CLEANUP_REGISTRY_SQL}", context.postgres.analytical_pg_user, "-d ${context.registry.name}")
            } else {
                context.logger.error("Subscription operational_sub is disabled on analytical master!")
            }
            context.logger.info("Cleaning registry DB on operational cluster")
            context.postgres.psqlScript(context.postgres.masterPod, "/tmp/${CLEANUP_REGISTRY_SQL}", context.postgres.operational_pg_user, "-d ${context.registry.name}")

            context.logger.info("Cleaning process_history DB on operational cluster")
            context.postgres.psqlScript(context.postgres.masterPod, "/tmp/${CLEANUP_PROCESS_HISTORY_SQL}", context.postgres.operational_pg_user)

            context.platform.scale("deployment/${BusinessProcMgmtSys.BPMS_DEPLOYMENT_NAME}", 1)
            context.platform.scale("deployment/${BusinessProcMgmtSys.BP_ADMIN_PORTAL_DEPLOYMENT_NAME}", 1)

            context.logger.info("Removing analytics roles from registry db on analytical cluster")
            LinkedHashMap officerUsersYaml = context.script.readYaml file: "roles/officer.yml"
            officerUsersYaml["roles"].each { String role ->
                if (role != null) {
                    try {
                        context.postgres.psqlCommand(context.postgres.masterRepPod,
                                "call p_delete_analytics_user('analytics_${role["name"]}')", context.registry.name, context.postgres.analytical_pg_user)
                    } catch (any) {
                        if (context.postgres.psqlCommand(context.postgres.masterRepPod,
                                "SELECT 1 FROM pg_roles WHERE rolname='analytics_${role["name"]}';",
                                context.registry.name, context.postgres.analytical_pg_user).trim() == '1') {
                            context.script.error("Removing of analytic role $role from database $context.registry.name FAILED")
                        }
                    }
                }
            }
        }
        parallelDeletion["removeRedashResources"] = {
            context.logger.info("Removing Redash resources")
            String cleanupRedashUsers = context.script.libraryResource("sql/${CLEANUP_REDASH_USERS_SQL}")
            context.script.writeFile(file: "sql_redash/${CLEANUP_REDASH_USERS_SQL}", text: cleanupRedashUsers)
            context.script.sh(script: "oc rsync --no-perms=true sql_redash/ ${REDASH_POD_NAME}:/tmp/")
            context.platform.podExec(REDASH_POD_NAME,
                    "bash -c \'export PGPASSWORD=${context.platform.getSecretValue("redash-secrets", "postgresqlPassword")}; psql redash -U redash -f tmp/${CLEANUP_REDASH_USERS_SQL}\'", "")
            context.redash.deleteRedashResource("${context.redash.viewerUrl}/api/dashboards",
                    context.redash.viewerApiKey)
            context.redash.deleteRedashResource("${context.redash.viewerUrl}/api/data_sources",
                    context.redash.viewerApiKey)
            context.redash.deleteRedashResource("${context.redash.viewerUrl}/api/groups", context.redash.viewerApiKey)
            context.logger.info("Remove audit dashboards job")
            try {
                context.script.sh(script: "oc delete job create-dashboard-job -n $context.namespace")
            } catch (any) {
                context.logger.info("Audit dashboards job removed already")
            }
        }
        parallelDeletion["removeKafkaTopics"] = {
            String kafkaBrokerPod = "kafka-cluster-kafka-0"
            String kafkaBootstrapServer = "kafka-cluster-kafka-bootstrap:9092"
            def kafkaTopicList
            int attempt = 0
            int maxAttempts = 10
            Boolean kafkaTopicsRemoved = false
            while (!kafkaTopicsRemoved) {
                attempt++
                if (attempt > maxAttempts) {
                    context.script.error("Attempts limit is reached and kafka topics were not removed yet!")
                    kafkaTopicsRemoved = true
                }
                String kafkaTopics = ''
                kafkaTopicList = context.script.sh(script: "oc exec $kafkaBrokerPod -c kafka -- bin/kafka-topics.sh " +
                        "--list --bootstrap-server $kafkaBootstrapServer", returnStdout: true).tokenize()
                kafkaTopicList.each { kafkaTopic ->
                    if (kafkaTopic.matches(".*\\d.*") && (kafkaTopic.contains("inbound") || kafkaTopic.contains("outbound"))) {
                        kafkaTopics = kafkaTopics + "$kafkaTopic,"
                    }
                }
                if (kafkaTopics.length() > 1) {
                    try {
                        context.script.sh(script: "oc exec $kafkaBrokerPod -c kafka -- bin/kafka-topics.sh " +
                                "--bootstrap-server $kafkaBootstrapServer --delete --topic ${kafkaTopics.substring(0, kafkaTopics.length() - 1)}")
                    } catch (any) {
                        kafkaTopicsRemoved = false
                        context.logger.info("Removing of kafka topics failed. Retrying (attempt $attempt/10)")
                    }
                } else {
                    kafkaTopicsRemoved = true
                }
            }
            context.logger.info("Kafka topics were successfully removed.")
        }
        context.script.parallel(parallelDeletion)
    }
}
