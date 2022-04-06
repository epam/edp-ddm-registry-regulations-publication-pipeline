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
            String token = context.keycloak.getAccessToken(context.jenkinsDeployer)
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
