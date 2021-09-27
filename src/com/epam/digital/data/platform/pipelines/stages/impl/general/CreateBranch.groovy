package com.epam.digital.data.platform.pipelines.stages.impl.general

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "create-branch", buildTool = ["any"], type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class CreateBranch {
    BuildContext context

    void run() {
        context.script.sshagent(["${context.gitServer.credentialsId}"]) {
            try {
                context.gitClient.gitSetConfig()
                context.script.sh(script: "git branch ${context.getParameterValue("BRANCH")}")
                context.gitClient.gitPush()
            }
            catch (any) {
                context.script.error "Create branch has failed with exception"
            }
        }
    }
}
