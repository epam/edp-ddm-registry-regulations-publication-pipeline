package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "create-redash-snippets", buildTool = ["any"], type = [ProjectType.LIBRARY])
class CreateRedashSnippets {
    BuildContext context

    private final String REDASH_PUBLISHER_JAR = "/home/jenkins/report-publisher/report-publisher.jar"

    void run() {
        if (context.registryRegulations.filesToDeploy.get(RegulationType.REPORTS)) {
        def response = context.script.httpRequest url: "${context.redash.adminUrl}/api/query_snippets",
                httpMode: "GET",
                contentType: "APPLICATION_JSON",
                customHeaders: [[name: "Authorization", value: context.redash.adminApiKey, maskValue: true]],
                consoleLogResponseBody: context.logLevel == "DEBUG",
                quiet: context.logLevel != "DEBUG",
                validResponseCodes: "200"
        if (response.content == "[]") {
            try {
                context.logger.info("Publishing redash snippets")
                context.script.sh(script: "java -jar " +
                        "-DREDASH_URL=${context.redash.adminUrl} " +
                        "-DREDASH_API_KEY=${context.redash.adminApiKey} " +
                        "-DPOSTGRES_PASSWORD=${context.citus.password} " +
                        "-DPOSTGRES_USER=${context.citus.user} " +
                        "-DDB_NAME=${context.registry.name} " +
                        "-DDB_URL=${context.citus.CITUS_MASTER_REP_URL} " +
                        "-DDB_PORT=${context.citus.CITUS_MASTER_PORT} " +
                        "-DPWD_ADMIN=${context.citus.analyticsAdminRolePass} " +
                        "-DPWD_AUDITOR=${context.citus.auditRolePass} " +
                        "${REDASH_PUBLISHER_JAR} " +
                        "--reports " +
                        "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
                context.logger.info("Redash snippets have been successfully published")
            }
            catch (any) {
                context.logger.warn("Publishing snippets failed")
            }
            context.logger.info("Redash snippets creation have been finished")
        } else {
            context.logger.info("Redash snippets are already published")
        }
        } else {
            context.logger.info("Skip redash snippets creation due to no changes")
        }
    }
}
