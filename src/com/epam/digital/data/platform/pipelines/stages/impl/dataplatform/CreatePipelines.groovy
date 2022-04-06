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

package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage
import jenkins.model.Jenkins

@Stage(name = "create-pipelines", buildTool = ["any"], type = [ProjectType.LIBRARY])
class CreatePipelines {
    BuildContext context

    void run() {
        context.dataComponents.values().each { dataComponent ->
            context.script.retry(3) {
                if (!Jenkins.getInstanceOrNull().getItemByFullName(dataComponent.pipelineName)) {
                    context.logger.info("Generating pipeline for ${dataComponent.name}")
                    context.script.build job: "job-provisions/ci/${dataComponent.jobProvisioner}",
                            wait: true, propagate: true, parameters: [
                            [$class: 'StringParameterValue', name: 'NAME', value: dataComponent.codebaseName],
                            [$class: 'StringParameterValue', name: 'BUILD_TOOL', value: dataComponent.BUILD_TOOL],
                            [$class: 'StringParameterValue', name: 'DEFAULT_BRANCH', value: dataComponent.codebaseBranch],
                            [$class: 'StringParameterValue', name: 'BRANCH', value: dataComponent.codebaseBranch],
                            [$class: 'StringParameterValue', name: 'REPOSITORY_PATH', value: dataComponent.repositoryPath],
                            [$class: 'StringParameterValue', name: 'GIT_CREDENTIALS_ID', value: context.gitServer.credentialsId],
                    ]
                }
            }
        }
    }
}
