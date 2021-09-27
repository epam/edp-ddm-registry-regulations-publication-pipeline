package com.epam.digital.data.platform.pipelines.stages.impl.general

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "config-files-validation", buildTool = ["any"], type = [ProjectType.LIBRARY])
class ConfigFilesValidation {
    BuildContext context

    private final String VALIDATOR_JAR = "/home/jenkins/registry-regulations-validator/registry-regulations-validator.jar"

    void run() {
        context.logger.info("Registry config files validation")
        try {
            context.script.sh(script: "java -jar ${VALIDATOR_JAR} ${context.logLevel == "DEBUG" ? "1>&2" : ""}")
        }
        catch (any) {
            context.script.error("Registry configs did not pass validation")
        }
        context.logger.info("Registry config files have been successfully validated")
    }
}
