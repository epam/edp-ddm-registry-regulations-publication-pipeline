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

package com.epam.digital.data.platform.pipelines.tools

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class GitClient {
    BuildContext context

    GitClient(BuildContext context) {
        this.context = context
    }

    void checkout(String url, String branch, String credentialsId) {
        context.script.checkout([$class                           : 'GitSCM',
                                 branches                         : [[name: branch]],
                                 doGenerateSubmoduleConfigurations: false, extensions: [],
                                 submoduleCfg                     : [],
                                 userRemoteConfigs                : [[credentialsId: credentialsId,
                                                                      url          : url]]])
    }

    void gerritCheckout(String url, String branch, String refspecName, String credentialsId) {
        context.script.checkout([$class                           : 'GitSCM',
                                 branches                         : [[name: branch]],
                                 doGenerateSubmoduleConfigurations: false, extensions: [],
                                 submoduleCfg                     : [],
                                 userRemoteConfigs                : [[refspec      : "${refspecName}:${branch}",
                                                                      credentialsId: credentialsId,
                                                                      url          : url]]])
    }

    void gitSetConfig() {
        context.script.sh(script: "git config --global user.email ${context.gitServer.autouser}@epam.com; " +
                "git config --global user.name ${context.gitServer.autouser}; " +
                "git config --global push.default matching")
    }

    void gitCommit(final String message) {
        context.script.sh(script: "git commit -m \"${message}\"")
    }

    void gitAdd(final String path = "-A") {
        context.script.sh(script: "git add ${path}")
    }

    void gitPush(String branch = "--all") {
        context.script.sh(script: "git push origin ${branch}")
    }

    void gitPushForce(String branch = "--all") {
        context.script.sh(script: "git push --force origin ${branch}")
    }

    void gitResetHardToPreviousCommit() {
        context.script.sh(script: "git reset --hard HEAD~1")
    }

    String getCurrentCommitMessage() {
        return context.script.sh(script: "git log -1 --format=%s", returnStdout: true)
    }
}
