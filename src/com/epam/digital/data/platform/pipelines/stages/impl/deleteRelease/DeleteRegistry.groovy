/*
 * Copyright 2021 EPAM Systems.
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
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.FormManagement
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage
import com.epam.digital.data.platform.pipelines.tools.TemplateRenderer

@Stage(name = "delete-registry", buildTool = ["any"], type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class DeleteRegistry {
    BuildContext context

    private String CLEANUP_REGISTRY_SQL = "CleanupRegistry.sql"

    void run() {
        try {
            context.logger.info("Cleaning form provider DB")
            context.platform.scale("deployment/${FormManagement.PROVIDER_DEPLOYMENT_NAME}", 0)
            context.platform.podExec(FormManagement.PROVIDER_DB_POD, "mongo ${FormManagement.PROVIDER_DB_NAME} " +
                    "--authenticationDatabase ${FormManagement.AUTH_DATABASE} " +
                    "-u ${context.platform.getSecretValue(FormManagement.PROVIDER_DB_SECRET, "username")} " +
                    "-p ${context.platform.getSecretValue(FormManagement.PROVIDER_DB_SECRET, "password")} " +
                    "--eval \"db.dropDatabase()\"", FormManagement.PROVIDER_DB_CONTAINER)
            context.platform.scale("deployment/${FormManagement.PROVIDER_DEPLOYMENT_NAME}", 1)

            context.logger.info("Cleaning form modeler DB")
            context.platform.scale("deployment/${FormManagement.MODELER_DEPLOYMENT_NAME}", 0)
            context.platform.podExec(FormManagement.MODELER_DB_POD, "mongo ${FormManagement.MODELER_DB_NAME} " +
                    "--authenticationDatabase ${FormManagement.AUTH_DATABASE} " +
                    "-u ${context.platform.getSecretValue(FormManagement.MODELER_DB_SECRET, "username")} " +
                    "-p ${context.platform.getSecretValue(FormManagement.MODELER_DB_SECRET, "password")} " +
                    "--eval \"db.dropDatabase()\"", FormManagement.MODELER_DB_CONTAINER)
            context.platform.scale("deployment/${FormManagement.MODELER_DEPLOYMENT_NAME}", 1)
        } catch (any) {
            context.logger.warn("There was an error during form management databases cleanup")
        }

        context.platform.scale("deployment/${BusinessProcMgmtSys.BPMS_DEPLOYMENT_NAME}", 0)
        context.platform.scale("deployment/${BusinessProcMgmtSys.BP_ADMIN_PORTAL_DEPLOYMENT_NAME}", 0)
        Map binding = ["OWNER_ROLE": context.citus.ownerRole]
        String template = context.script.libraryResource("sql/${CLEANUP_REGISTRY_SQL}")
        context.script.writeFile(file: "sql/${CLEANUP_REGISTRY_SQL}", text: TemplateRenderer.renderTemplate(template, binding))

        context.logger.info("Cleaning registry DB on replica")
        context.script.sh(script: "oc rsync sql/ ${context.citus.masterRepPod}:/tmp/")
        context.citus.psqlScript(context.citus.masterRepPod, "/tmp/${CLEANUP_REGISTRY_SQL}", "-d ${context.registry.name}")

        context.logger.info("Cleaning registry DB on master")
        context.script.sh(script: "oc rsync sql/ ${context.citus.masterPod}:/tmp/")
        context.citus.psqlScript(context.citus.masterPod, "/tmp/${CLEANUP_REGISTRY_SQL}", "-d ${context.registry.name}")

        context.platform.scale("deployment/${BusinessProcMgmtSys.BPMS_DEPLOYMENT_NAME}", 1)
        context.platform.scale("deployment/${BusinessProcMgmtSys.BP_ADMIN_PORTAL_DEPLOYMENT_NAME}", 1)

    }
}
