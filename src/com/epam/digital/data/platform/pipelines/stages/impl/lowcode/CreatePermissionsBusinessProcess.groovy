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

@Stage(name = "create-permissions-business-process", buildTool = ["any"], type = [ProjectType.LIBRARY])
class CreatePermissionsBusinessProcess {
    BuildContext context

    private final String CAMUNDA_AUTH_CLI = "/home/jenkins/camunda-auth-cli/camunda-auth-cli.jar"

    void run() {
        if (context.registryRegulations.deployStatus("create-permissions-business-process",
                "${RegulationType.BUSINESS_PROCESS_AUTH.value}")) {
            try {
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
                    context.script.writeFile(file: tokenFile,
                            text: context.keycloak.getAccessToken(context.jenkinsDeployer))
                    context.script.sh(script: "java -jar ${CAMUNDA_AUTH_CLI} " +
                            "--BPMS_URL=${BusinessProcMgmtSys.URL} " +
                            "--BPMS_TOKEN=${tokenFile} " +
                            "--AUTH_FILES=${filesList.join(",")} ${context.logLevel == "DEBUG" ? "1>&2" : ""};" +
                            "rm -f ${tokenFile}")
                }

            } catch (any) {
                context.logger.error("Error during creating business process permissions")
                context.stageFactory.runStage(context.RESTORE_STAGE, context)
            }
            context.registryRegulations.getChangedStatusOrFiles("save", "create-permissions-business-process",
                    "--file ${context.getWorkDir()}/${RegulationType.BUSINESS_PROCESS_AUTH.value}")
        } else {
            context.logger.info("Skip ${RegulationType.BUSINESS_PROCESS_AUTH.value}" +
                    " files upload due to empty change list")
        }
    }
}
