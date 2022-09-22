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

package com.epam.digital.data.platform.pipelines.stages.impl.upgrade

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "create-registry-settings-file", buildTool = ["any"], type = [ProjectType.LIBRARY])
class CreateRegistrySettingsFile {
    BuildContext context

    private final String COMMIT_MESSAGE = "Add registry settings yaml"

    void run() {
        context.script.sh("mkdir -p ${RegulationType.REGISTRY_SETTINGS.value}")
        context.script.dir(RegulationType.REGISTRY_SETTINGS.value) {
            if (!context.script.fileExists(context.registry.REGISTRY_SETTINGS_FILE)) {
                context.logger.info("Creating settings file")
                String template = context.script.libraryResource("templates/other/settings.yml")
                context.script.writeFile(file: context.registry.REGISTRY_SETTINGS_FILE, text: template)
                context.script.sshagent(["${context.gitServer.credentialsId}"]) {
                    context.script.sh "git checkout -b ${context.codebase.branch}"
                    context.gitClient.gitSetConfig()
                    context.gitClient.gitAdd()
                    context.gitClient.gitCommit(COMMIT_MESSAGE)
                    context.script.retry(5) {
                        context.gitClient.gitPush(context.codebase.branch)
                    }
                }
            } else {
                context.logger.info("Settings file already exists")
            }
        }
    }
}
