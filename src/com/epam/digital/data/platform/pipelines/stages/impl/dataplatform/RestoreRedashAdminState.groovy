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

package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "restore-redash-admin-state", buildTool = ["any"], type = [ProjectType.LIBRARY])
class RestoreRedashAdminState {
    BuildContext context

    private final String REDASH_PUBLISHER_JAR = "/home/jenkins/report-publisher/report-publisher.jar"

    void run() {
        if (context.registryRegulations.deployStatus("restore-redash-admin-state",
                "${RegulationType.REPORTS.value}")) {
            context.script.retry(5) {
                def response = context.script.httpRequest url: "${context.redash.adminUrl}/api/query_snippets",
                        httpMode: "GET",
                        contentType: "APPLICATION_JSON",
                        customHeaders: [[name: "Authorization", value: context.redash.adminApiKey, maskValue: true]],
                        consoleLogResponseBody: context.logLevel == "DEBUG",
                        quiet: context.logLevel != "DEBUG",
                        validResponseCodes: "200"
                context.logger.debug("Redash admin response: ${response.content}")
                if (response.content == "[]") {
                    try {
                        context.logger.info("Redash-admin is empty. Start restoring...")
                        context.script.sh(script: "set +x; java -jar " +
                                "-DREDASH_URL=${context.redash.adminUrl} " +
                                "-DREDASH_API_KEY=${context.redash.adminApiKey} " +
                                "-DPOSTGRES_PASSWORD=\'${context.postgres.analytical_pg_password}\' " +
                                "-DPOSTGRES_USER=${context.postgres.analytical_pg_user} " +
                                "-DDB_NAME=${context.registry.name} " +
                                "-DDB_URL=${context.postgres.ANALYTICAL_MASTER_URL} " +
                                "-DDB_PORT=${context.postgres.OPERATIONAL_MASTER_PORT} " +
                                "-DPWD_ADMIN=${context.postgres.analyticsAdminRolePass} " +
                                "-DPWD_AUDITOR=${context.postgres.auditRolePass} " +
                                "${REDASH_PUBLISHER_JAR} " +
                                "--reports " +
                                "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
                        context.logger.info("Redash-admin has been successfully restored!")
                    }
                    catch (any) {
                        context.script.error("Error during restoring Redash-admin(")
                    }
                    context.logger.info("Redash-admin restoring have been finished.")

                } else {
                    context.logger.info("Redash-admin is not empty. Skipping restoring.")
                }
            }
            context.registryRegulations.getChangedStatusOrFiles("save", "restore-redash-admin-state",
                    "--file ${context.getWorkDir()}/${RegulationType.REPORTS.value}")
        } else {
            context.logger.info("Skip restore-redash-admin-state due to no changes")
        }
    }
}
