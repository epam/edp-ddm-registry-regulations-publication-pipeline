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
