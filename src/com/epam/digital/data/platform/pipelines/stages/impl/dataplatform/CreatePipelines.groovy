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
