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

    private String CITUS_JOB_NAME = "run-db-scripts-job"
    private String CITUS_JOB_FILENAME = "citus-job.yaml"
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
        Map binding = ["REGISTRY_NAME": context.registry.name]
        String template = context.script.libraryResource("sql/${CLEANUP_REGISTRY_SQL}")
        context.script.writeFile(file: "sql/${CLEANUP_REGISTRY_SQL}", text: TemplateRenderer.renderTemplate(template, binding))

        context.logger.info("Cleaning Citus DB on replica")
        context.script.sh(script: "oc rsync sql/ ${context.citus.masterRepPod}:/tmp/")
        context.citus.psqlCommand(context.citus.masterRepPod, "DROP SUBSCRIPTION operational_sub;", context.registry.name)
        context.citus.psqlScript(context.citus.masterRepPod, "/tmp/${CLEANUP_REGISTRY_SQL}")

        context.logger.info("Cleaning Citus DB on master")
        context.script.sh(script: "oc rsync sql/ ${context.citus.masterPod}:/tmp/")
        context.citus.psqlCommand(context.citus.masterPod, "DROP PUBLICATION analytical_pub;", context.registry.name)
        context.citus.psqlScript(context.citus.masterPod, "/tmp/${CLEANUP_REGISTRY_SQL}")

        context.platform.scale("deployment/${BusinessProcMgmtSys.BPMS_DEPLOYMENT_NAME}", 1)
        context.platform.scale("deployment/${BusinessProcMgmtSys.BP_ADMIN_PORTAL_DEPLOYMENT_NAME}", 1)

        context.logger.info("Restoring registry DB objects")
        context.platform.get("job", CITUS_JOB_NAME,
                "-o yaml --export | sed \'/selector/d; /matchLabels/d; /controller-uid/d\' > $CITUS_JOB_FILENAME;")
        context.platform.deleteObject("job", CITUS_JOB_NAME)
        context.platform.apply(CITUS_JOB_FILENAME)
    }
}
