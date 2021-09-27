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
}
