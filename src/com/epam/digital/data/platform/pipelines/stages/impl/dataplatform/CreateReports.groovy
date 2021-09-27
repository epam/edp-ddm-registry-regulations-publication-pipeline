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
        if (context.registryRegulations.filesToDeploy.get(RegulationType.REPORTS)) {
            try {
                context.logger.info("Publishing redash ${RegulationType.REPORTS.value}")
                context.script.sh(script: "java -jar " +
                        "-DREDASH_URL=${context.redash.viewerUrl} " +
                        "-DREDASH_API_KEY=${context.redash.viewerApiKey} " +
                        "-DPOSTGRES_PASSWORD=${context.citus.password} " +
                        "-DPOSTGRES_USER=${context.citus.user} " +
                        "-DDB_NAME=${context.registry.name} " +
                        "-DDB_URL=${context.citus.CITUS_MASTER_URL} " +
                        "-DDB_PORT=${context.citus.CITUS_MASTER_PORT} " +
                        "-DPWD_ADMIN=${context.citus.analyticsAdminRolePass} " +
                        "-DPWD_AUDITOR=${context.citus.auditRolePass} " +
                        "${REDASH_PUBLISHER_JAR} " +
                        "--reports " +
                        "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
                context.logger.info("Redash ${RegulationType.REPORTS.value} have been successfully published")
            }
            catch (any) {
                context.logger.warn("Publishing ${RegulationType.REPORTS.value} failed")
            }
            context.logger.info("Redash ${RegulationType.REPORTS.value} creation have been finished")
        } else {
            context.logger.info("Skip redash ${RegulationType.REPORTS.value} publishing due to no changes")
        }
    }
}
