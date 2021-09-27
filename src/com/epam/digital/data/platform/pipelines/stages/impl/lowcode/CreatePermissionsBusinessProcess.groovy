package com.epam.digital.data.platform.pipelines.stages.impl.lowcode

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.BusinessProcMgmtSys
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "create-permissions-business-process", buildTool = ["any"], type = [ProjectType.LIBRARY])
class CreatePermissionsBusinessProcess {
    BuildContext context

    private final String CAMUNDA_AUTH_CLI = "/home/jenkins/camunda-auth-cli/camunda-auth-cli.jar"

    void run() {
        try {
            if (context.registryRegulations.filesToDeploy.get(RegulationType.BUSINESS_PROCESS_AUTH)) {
                context.logger.info("Creating ${RegulationType.BUSINESS_PROCESS_AUTH.value}")
                ArrayList<String> filesList = []
                context.registryRegulations.getAllRegulations(RegulationType.BUSINESS_PROCESS_AUTH).each { file ->
                    String fileContent = context.script.readFile(file: file)
                    if (fileContent.isEmpty()) {
                        context.logger.info("Skip empty ${file} auth file")
                    } else {
                        filesList.add(file)
                    }
                }

                String tokenFile = "token.txt"
                if (!filesList.isEmpty()) {
                    context.script.writeFile(file: tokenFile, text: context.keycloak.getDeployerAccessToken())
                    context.script.sh(script: "java -jar ${CAMUNDA_AUTH_CLI} " +
                            "--BPMS_URL=${BusinessProcMgmtSys.URL} " +
                            "--BPMS_TOKEN=${tokenFile} " +
                            "--AUTH_FILES=${filesList.join(",")} ${context.logLevel == "DEBUG" ? "1>&2" : ""};" +
                            "rm -f ${tokenFile}")
                }
            } else {
                context.logger.info("Skip ${RegulationType.BUSINESS_PROCESS_AUTH.value}" +
                        " files upload due to empty change list")
            }
        } catch (any) {
            context.logger.error("Error during creating business process permissions")
            context.stageFactory.runStage(context.RESTORE_STAGE, context)
        }
    }
}
