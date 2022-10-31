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

package com.epam.digital.data.platform.pipelines.stages.impl.general

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "trigger-job", buildTool = ["any"], type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class TriggerJob {
    BuildContext context

    void run() {
        String triggerJobName = "job-provisions/ci/${context.codebase.jobProvisioner}"
        context.script.build job: triggerJobName, wait: true, propagate: true, parameters: [
                [$class: 'StringParameterValue', name: 'NAME', value: context.codebase.name],
                [$class: 'StringParameterValue', name: 'BUILD_TOOL', value: context.codebase.buildToolSpec],
                [$class: 'StringParameterValue', name: 'DEFAULT_BRANCH', value: context.codebase.defaultBranch],
                [$class: 'StringParameterValue', name: 'BRANCH', value: context.codebase.branch],
                [$class: 'StringParameterValue', name: 'REPOSITORY_PATH', value: context.codebase.repositoryPath],
                [$class: 'StringParameterValue', name: 'GIT_CREDENTIALS_ID', value: context.gitServer.credentialsId],
                [$class: 'StringParameterValue', name: 'DEPLOYMENT_MODE', value: context.deploymentMode]
        ]
    }
}
