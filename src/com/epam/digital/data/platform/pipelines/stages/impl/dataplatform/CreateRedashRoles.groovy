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

@Stage(name = "create-redash-roles", buildTool = ["any"], type = [ProjectType.LIBRARY])
class CreateRedashRoles {
    BuildContext context

    private final String REDASH_PUBLISHER_JAR = "/home/jenkins/report-publisher/report-publisher.jar"

    void run() {
        if (context.registryRegulations.filesToDeploy.get(RegulationType.ROLES)) {
            context.logger.info("Creating redash ${RegulationType.ROLES.value}")

            context.logger.info("Creating admin role on replica for redash admin instance")
            createRoles("admin", context.redash.adminUrl, context.redash.adminApiKey)
            context.logger.info("Admin role has been successfully created")

            context.logger.info("Creating rest of roles on replica for redash viewer instance")
            createRoles("roles", context.redash.viewerUrl, context.redash.viewerApiKey)
            context.logger.info("Other roles have been successfully created")

            context.logger.info("Redash ${RegulationType.ROLES.value} creation have been finished")
        } else {
            context.logger.info("Skip redash ${RegulationType.ROLES.value} creation due to no changes")
        }
    }

    private void createRoles(String roles, String redashUrl, String redashApiKey) {
        context.logger.info("Creating redash ${RegulationType.ROLES.value}")
        try {
            context.script.sh(script: "java -jar " +
                    "-DREDASH_URL=${redashUrl} " +
                    "-DREDASH_API_KEY=${redashApiKey} " +
                    "-DPOSTGRES_PASSWORD=${context.citus.password} " +
                    "-DPOSTGRES_USER=${context.citus.user} " +
                    "-DDB_NAME=${context.registry.name} " +
                    "-DDB_URL=${context.citus.CITUS_MASTER_REP_URL} " +
                    "-DDB_PORT=${context.citus.CITUS_MASTER_REP_PORT} " +
                    "-DPWD_ADMIN=${context.citus.analyticsAdminRolePass} " +
                    "-DPWD_AUDITOR=${context.citus.auditRolePass} " +
                    "${REDASH_PUBLISHER_JAR} " +
                    "--${roles} " +
                    "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
            context.logger.info("Role(s) has been successfully created")
        }
        catch (any) {
            context.script.error("Role(s) creation failed")
        }
    }
}
