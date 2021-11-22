/*
 * Copyright 2021 EPAM Systems.
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

package com.epam.digital.data.platform.pipelines.registrycomponents.regular

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class Gerrit extends GitServer {
    Gerrit(BuildContext context, String name) {
        super(context, name)
    }

    @Override
    boolean isRepositoryExists(String repoName) {
        context.logger.debug("Checking if ${repoName} exists")
        context.script.sshagent(["${credentialsId}"]) {
            ArrayList<String> gerritRepositories = context.script.sh(script: "ssh -p ${sshPort} ${autouser}@${host} " +
                    "gerrit ls-projects", returnStdout: true).tokenize('\n')
            boolean isExists = gerritRepositories.find { it == repoName } ? true : false
            context.logger.debug("Exists: ${isExists}")
            return isExists
        }
    }

    @Override
    boolean deleteRepository(String repoName) {
        context.script.sshagent(["${credentialsId}"]) {
            context.script.sh(script: "ssh -oStrictHostKeyChecking=no " +
                    "-p ${sshPort} ${autouser}@${host} " +
                    "delete-project delete --yes-really-delete --force ${repoName}")
        }
    }
}
