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
        ArrayList<String> changedBPFiles
        if (context.getParameterValue("FULL_DEPLOY", "false").toBoolean()) {
            changedBPFiles = context.registryRegulations.getAllRegulations(RegulationType.BUSINESS_PROCESS).join(",").tokenize(',') +
                    context.registryRegulations.getAllRegulations(RegulationType.BUSINESS_RULE).join(",").tokenize(',')
        } else {
            changedBPFiles = context.registryRegulations.getChangedStatusOrFiles("plan",
                    "upload-business-process-changes", "--file-detailed ${context.getWorkDir()}/${RegulationType.BUSINESS_PROCESS.value}," +
                    "${context.getWorkDir()}/${RegulationType.BUSINESS_RULE.value}")
        }
        if (changedBPFiles) {
            String token = context.keycloak.getAccessToken(context.jenkinsDeployer)
            changedBPFiles.each {
                if (!it.contains(".gitkeep")) {
                    deploy(it, token)
                }
            }
            context.registryRegulations.getChangedStatusOrFiles("save",
                    "upload-business-process-changes", "--file-detailed ${context.getWorkDir()}/${RegulationType.BUSINESS_PROCESS.value}," +
                    "${context.getWorkDir()}/${RegulationType.BUSINESS_RULE.value}")

        } else {
            context.logger.info("Skip ${RegulationType.BUSINESS_PROCESS.value} and ${RegulationType.BUSINESS_RULE.value} files deploy due to empty change list")
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
            context.logger.error("Failed to deploy ${file}")
        }
    }
}
