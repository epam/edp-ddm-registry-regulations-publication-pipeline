package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "import-excerpts", buildTool = ["any"], type = [ProjectType.LIBRARY])
class ImportExcerpts {
    BuildContext context

    private final String EXCERPTS_IMPORTER_JAR = "/home/jenkins/report-publisher/report-publisher.jar"
    private final String EXCERPTS_DB_NAME = "excerpt"

    void run() {
        if (context.registryRegulations.filesToDeploy.get(RegulationType.EXCERPTS)) {
            try {
                context.logger.info("Importing excerpts")
                context.script.sh(script: "java -jar " +
                        "-DREDASH_URL=${context.redash.viewerUrl} " +
                        "-DREDASH_API_KEY=${context.redash.viewerApiKey} " +
                        "-DPOSTGRES_PASSWORD=${context.citus.excerptExporterPass} " +
                        "-DPOSTGRES_USER=${context.citus.excerptExporterUser} " +
                        "-DDB_NAME=${EXCERPTS_DB_NAME} " +
                        "-DDB_URL=${context.citus.CITUS_MASTER_URL} " +
                        "-DDB_PORT=${context.citus.CITUS_MASTER_PORT} " +
                        "-DPWD_ADMIN=${context.citus.analyticsAdminRolePass} " +
                        "-DPWD_AUDITOR=${context.citus.auditRolePass} " +
                        "${EXCERPTS_IMPORTER_JAR} " +
                        "--excerpts " +
                        "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
                context.logger.info("Excerpts have been successfully imported")
            } catch (any) {
                context.logger.warn("Excerpts import have been failed")
            }
        } else {
            context.logger.info("Skip ${RegulationType.EXCERPTS.value} import due to no changes")
        }
    }
}
