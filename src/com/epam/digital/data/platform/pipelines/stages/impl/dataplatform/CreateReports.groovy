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

@Stage(name = "create-reports", buildTool = ["any"], type = [ProjectType.LIBRARY])
class CreateReports {
    BuildContext context

    private final String REDASH_PUBLISHER_JAR = "/home/jenkins/report-publisher/report-publisher.jar"

    void run() {
        if (context.registryRegulations.deployStatus("create-reports", "${RegulationType.REPORTS.value}")) {
            try {
                context.logger.info("Publishing redash ${RegulationType.REPORTS.value}")
                context.script.sh(script: "set +x; java -jar " +
                        "-DREDASH_URL=${context.redash.viewerUrl} " +
                        "-DREDASH_API_KEY=${context.redash.viewerApiKey} " +
                        "-DPOSTGRES_PASSWORD=\'${context.postgres.operational_pg_password}\' " +
                        "-DPOSTGRES_USER=${context.postgres.operational_pg_user} " +
                        "-DDB_NAME=${context.registry.name} " +
                        "-DDB_URL=${context.postgres.OPERATIONAL_MASTER_URL} " +
                        "-DDB_PORT=${context.postgres.OPERATIONAL_MASTER_PORT} " +
                        "-DPWD_ADMIN=${context.postgres.analyticsAdminRolePass} " +
                        "-DPWD_AUDITOR=${context.postgres.auditRolePass} " +
                        "${REDASH_PUBLISHER_JAR} " +
                        "--reports " +
                        "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
                context.logger.info("Redash ${RegulationType.REPORTS.value} have been successfully published")
            }
            catch (any) {
                context.script.error("Publishing ${RegulationType.REPORTS.value} failed")
            }
            context.logger.info("Redash ${RegulationType.REPORTS.value} creation have been finished")
                context.registryRegulations.getChangedStatusOrFiles("save", "create-reports",
                        "--file ${context.getWorkDir()}/${RegulationType.REPORTS.value}")
        } else {
            context.logger.info("Skip redash ${RegulationType.REPORTS.value} publishing due to no changes")
        }
    }
}

