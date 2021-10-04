package com.epam.digital.data.platform.pipelines.stages.impl.general

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "registry-regulations-validation", buildTool = ["any"], type = [ProjectType.LIBRARY])
class RegistryRegulationsValidation {
    BuildContext context

    private final String LOWCODE_VALIDATOR_JAR = "/home/jenkins/registry-regulations-validator-cli/registry-regulations-validator-cli.jar"
    private final String DATA_VALIDATOR_JAR = "/home/jenkins/registry-regulations-validator/registry-regulations-validator.jar"

    void run() {
        context.logger.info("Registry regulations files validation")
        try {
            context.script.sh(script: "java -jar ${DATA_VALIDATOR_JAR} ${context.logLevel == "DEBUG" ? "1>&2" : ""}")
            context.script.sh(script: "java -jar ${LOWCODE_VALIDATOR_JAR} " +
                    "--bp-auth-files=${context.registryRegulations.filesToDeploy.get(RegulationType.BUSINESS_PROCESS_AUTH).join(",")} " +
                    "--bp-trembita-files=${context.registryRegulations.filesToDeploy.get(RegulationType.BUSINESS_PROCESS_TREMBITA).join(",")} " +
                    "--bpmn-files=${context.registryRegulations.filesToDeploy.get(RegulationType.BUSINESS_PROCESS).join(",")} " +
                    "--dmn-files=${context.registryRegulations.filesToDeploy.get(RegulationType.BUSINESS_RULE).join(",")} " +
                    "--form-files=${context.registryRegulations.filesToDeploy.get(RegulationType.UI_FORM).join(",")} " +
                    "--global-vars-files=${context.registryRegulations.filesToDeploy.get(RegulationType.GLOBAL_VARS).join(",")} " +
                    "--roles-files=${context.registryRegulations.filesToDeploy.get(RegulationType.ROLES).join(",")} " +
                    "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
        }
        catch (any) {
            context.script.error("Registry regulations files did not pass validation")
        }
        context.logger.info("Registry regulations files have been successfully validated")
    }
}
