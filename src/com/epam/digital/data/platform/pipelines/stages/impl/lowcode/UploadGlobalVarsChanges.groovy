package com.epam.digital.data.platform.pipelines.stages.impl.lowcode

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.BusinessProcMgmtSys
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "upload-global-vars-changes", buildTool = ["any"], type = ProjectType.LIBRARY)
class UploadGlobalVarsChanges {
    BuildContext context

    void run() {
        context.script.dir("${context.workDir}/${RegulationType.GLOBAL_VARS.value}") {
            try {
                context.logger.info("Updating camunda global vars")
                String CAMUNDA_GLOBAL_VARS_FILE = "camunda-global-system-vars.yml"
                String camundaGlobalVarsYaml = "camunda:\\n  system-variables:\\n" +
                        "${context.script.sh(script: """x=4; awk '{printf "%"'\$x'"s%s_%s\\n", "", "const", \$0}' \
                        ${CAMUNDA_GLOBAL_VARS_FILE}""", returnStdout: true).replaceAll("\n", "\\\\n")}"
                applyChanges(BusinessProcMgmtSys.GLOBAL_VARS_CONFIG_MAP, CAMUNDA_GLOBAL_VARS_FILE, camundaGlobalVarsYaml,
                        BusinessProcMgmtSys.BPMS_DEPLOYMENT_NAME)
                context.logger.info("Camunda global have been successfully updated")

                context.logger.info("Updating registry env variables for portals")
                String asJson = context.script.sh(script: """x=2; awk '{printf "%"'\$x'"s%s%s\\n", "", \$0, ","}' \
                        ${CAMUNDA_GLOBAL_VARS_FILE}""", returnStdout: true)
                        .replaceAll("\n", "\\\\n")
                        .replaceAll(': ', ': \'')
                        .replaceAll(',', '\',')
                String jsRegistryEnvVarsJson = "const REGISTRY_ENVIRONMENT_VARIABLES = {\\n  ${asJson};"
                applyChanges("registry-environment-js", "registry-environment.js", jsRegistryEnvVarsJson,
                        "citizen-portal,officer-portal")
                context.logger.info("Registry env variables have been successfully updated")
            } catch (any) {
                context.logger.error("Error during uploading global variables changes")
                context.stageFactory.runStage(context.RESTORE_STAGE, context)
            }
        }
    }

    private void applyChanges(String configMapName, String key, String value, String deploymentName) {
        if (context.platform.patchConfigMapKey(configMapName, key, value)) {
            context.logger.info("Config map ${configMapName} is changed. Triggering rollout of ${deploymentName}")
            context.platform.triggerDeploymentRollout(deploymentName)
        } else {
            context.logger.info("Config map ${configMapName} is not changed")
        }
    }
}
