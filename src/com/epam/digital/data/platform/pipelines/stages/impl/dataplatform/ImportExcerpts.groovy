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
import com.epam.digital.data.platform.pipelines.registrycomponents.external.CephBucket
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "import-excerpts", buildTool = ["any"], type = [ProjectType.LIBRARY])
class ImportExcerpts {
    BuildContext context
    private final String EXCERPTS_IMPORTER_JAR = "/home/jenkins/report-publisher/report-publisher.jar"
    private final String EXCERPTS_DB_NAME = "excerpt"

    void run() {
        if (context.registryRegulations.filesToDeploy.get(RegulationType.EXCERPTS)) {
            CephBucket excerptBucket = new CephBucket("excerpt-templates", context)
            excerptBucket.init()
            try {
                context.logger.info("Importing excerpts")
                context.script.sh(script: "set +x; java -jar " +
                        "-DREDASH_URL=${context.redash.viewerUrl} " +
                        "-DREDASH_API_KEY=${context.redash.viewerApiKey} " +
                        "-DPOSTGRES_PASSWORD=${context.citus.excerptExporterPass} " +
                        "-DPOSTGRES_USER=${context.citus.excerptExporterUser} " +
                        "-DDB_NAME=${EXCERPTS_DB_NAME} " +
                        "-DDB_URL=${context.citus.CITUS_MASTER_URL} " +
                        "-DDB_PORT=${context.citus.CITUS_MASTER_PORT} " +
                        "-DPWD_ADMIN=${context.citus.analyticsAdminRolePass} " +
                        "-DPWD_AUDITOR=${context.citus.auditRolePass} " +
                        "-DCEPH_BUCKET=${excerptBucket.cephBucketName} " +
                        "-DCEPH_HTTP_ENDPOINT=${excerptBucket.cephHttpEndpoint} " +
                        "-DCEPH_ACCESS_KEY=${excerptBucket.cephAccessKey} " +
                        "-DCEPH_SECRET_KEY=${excerptBucket.cephSecretKey} " +
                        "${EXCERPTS_IMPORTER_JAR} " +
                        "--excerpts --excerpts-docx --excerpts-csv " +
                        "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
                context.logger.info("Excerpts have been successfully imported")
            } catch (any) {
                context.script.error("Excerpts import have been failed")
            }
        } else {
            context.logger.info("Skip ${RegulationType.EXCERPTS.value} import due to no changes")
        }
    }
}
