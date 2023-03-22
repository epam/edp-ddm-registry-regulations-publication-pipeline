/*
 * Copyright 2023 EPAM Systems.
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

package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registrycomponents.generated.DataComponentType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage
import com.epam.digital.data.platform.pipelines.tools.Helm

@Stage(name = "delete-data-services", buildTool = ["any"], type = [ProjectType.LIBRARY])
class DeleteDataServices {
    BuildContext context

    void run() {
        context.dataComponents.values().each { dataComponent ->
            context.logger.info("Removing data components buildconfigs")
            String buildConfigName = dataComponent.codebaseName + '-' + dataComponent.codebaseBranch
            if (context.platform.checkObjectExists("buildconfig", buildConfigName)) {
                context.platform.deleteObject("buildconfig", buildConfigName)
            } else {
                context.logger.info("There is no buildconfig " + context.codebase.buildConfigName)
            }
            context.script.dir("${context.workDir}/${dataComponent.name}") {
                context.script.sshagent(["${context.gitServer.credentialsId}"]) {
                    context.logger.info("Checkout ${dataComponent.fullName}")
                    context.gitClient.gitSetConfig()
                    context.gitClient.checkout(dataComponent.repositoryPath, dataComponent.codebaseBranch,
                            context.gitServer.credentialsId)
                    context.script.sh(script: "git checkout ${dataComponent.codebaseBranch}")
                    String currentCommitMessage = context.gitClient.getCurrentCommitMessage()
                    if (currentCommitMessage.contains("generated")) {
                        Helm.uninstall(context, dataComponent.fullName, context.namespace, true)
                        if (dataComponent.name == DataComponentType.REST_API.getValue()) {
                            try {
                                context.logger.info("Removing kafka topics")
                                context.kafka.removeKafkaTopics()
                            } catch (any) {
                                context.script.error("Cannot gracefully remove kafka topics")
                            }
                        }
                        context.gitClient.gitResetHardToPreviousCommit()
                        context.gitClient.gitPushForce(dataComponent.codebaseBranch)
                    }
                }
            }
        }
    }
}
