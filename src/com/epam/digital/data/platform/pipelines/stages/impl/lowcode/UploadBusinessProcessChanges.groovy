package com.epam.digital.data.platform.pipelines.stages.impl.lowcode

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.BusinessProcMgmtSys
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "upload-business-process-changes", buildTool = ["any"], type = ProjectType.LIBRARY)
class UploadBusinessProcessChanges {
    BuildContext context

    void run() {
        ArrayList<String> changedBPFiles = context.registryRegulations.filesToDeploy.get(
                RegulationType.BUSINESS_PROCESS) + context.registryRegulations.filesToDeploy.get(
                RegulationType.BUSINESS_RULE)
        if (changedBPFiles) {
            String token = context.keycloak.getDeployerAccessToken()
            changedBPFiles.each {
                deploy(it, token)
            }
        } else {
            context.logger.info("Skip ${RegulationType.BUSINESS_PROCESS.value} files deploy due to empty change list")
        }
    }

    private void deploy(String file, String token) {
        try {
            context.logger.info("Deploying ${file}")
            context.script.httpRequest url: "${BusinessProcMgmtSys.URL}/${BusinessProcMgmtSys.DEPLOY_API_PATH}",
                    httpMode: 'POST',
                    uploadFile: file,
                    contentType: 'APPLICATION_OCTETSTREAM',
                    wrapAsMultipart: true,
                    multipartName: file,
                    consoleLogResponseBody: context.logLevel == "DEBUG",
                    quiet: context.logLevel != "DEBUG",
                    customHeaders: [[maskValue: true, name: 'X-Access-Token', value: token]],
                    validResponseCodes: "200"
            context.logger.info("${file} have been succussfully deployed")
        } catch (any) {
            context.logger.warn("Failed to deploy ${file}")
        }
    }
}
