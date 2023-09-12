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
        ["${RegulationType.EXCERPTS.value}", "${RegulationType.EXCERPTS.value}-docx", "${RegulationType.EXCERPTS.value}-csv"].each {
            if (context.registryRegulations.deployStatus("import-$it",
                    "${it}")) {
                CephBucket excerptBucket = new CephBucket("excerpt-templates", context)
                excerptBucket.init()
                importExcerpts(it, "${excerptBucket.cephBucketName}", "${excerptBucket.cephHttpEndpoint}",
                        "${excerptBucket.cephAccessKey}", "${excerptBucket.cephSecretKey}")
                context.registryRegulations.getChangedStatusOrFiles("save", "import-$it",
                        "--file ${context.getWorkDir()}/$it")
            } else {
                context.logger.info("Skip ${it} import due to no changes")
            }
        }
    }

    private void importExcerpts(String dirName, String cephBucketName, String cephHttpEndpoint, String cephAccessKey, String cephSecretKey) {
        context.logger.info("Importing excerpts for ${dirName}")
        try {
            context.script.sh(script: "set +x; java -jar " +
                    "-DREDASH_URL=${context.redash.viewerUrl} " +
                    "-DREDASH_API_KEY=${context.redash.viewerApiKey} " +
                    "-DPOSTGRES_PASSWORD=${context.postgres.excerptExporterPass} " +
                    "-DPOSTGRES_USER=${context.postgres.excerptExporterUser} " +
                    "-DDB_NAME=${EXCERPTS_DB_NAME} " +
                    "-DDB_URL=${context.postgres.OPERATIONAL_MASTER_URL} " +
                    "-DDB_PORT=${context.postgres.OPERATIONAL_MASTER_PORT} " +
                    "-DPWD_ADMIN=${context.postgres.analyticsAdminRolePass} " +
                    "-DPWD_AUDITOR=${context.postgres.auditRolePass} " +
                    "-DCEPH_BUCKET=${cephBucketName} " +
                    "-DCEPH_HTTP_ENDPOINT=${cephHttpEndpoint} " +
                    "-DCEPH_ACCESS_KEY=${cephAccessKey} " +
                    "-DCEPH_SECRET_KEY=${cephSecretKey} " +
                    "${EXCERPTS_IMPORTER_JAR} " +
                    "--${dirName} " +
                    "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
            context.logger.info("Excerpts have been successfully imported for ${dirName}")

        } catch (any) {
            context.script.error("Excerpts import have been failed for ${dirName}")
        }
    }
}
