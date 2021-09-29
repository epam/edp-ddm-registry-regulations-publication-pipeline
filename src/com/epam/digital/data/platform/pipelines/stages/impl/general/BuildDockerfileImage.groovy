package com.epam.digital.data.platform.pipelines.stages.impl.general

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage
import com.epam.digital.data.platform.pipelines.tools.TemplateRenderer

@Stage(name = "build-image-from-dockerfile", buildTool = ["any"], type = [ProjectType.APPLICATION])
class BuildDockerfileImage {
    BuildContext context

    String BUILD_CONFIG_API = "buildconfig"

    void run() {
        if (context.script.fileExists("${context.workDir}/Dockerfile")) {
            String globalNexusNamespace = context.dnsWildcard.startsWith("apps.cicd") ?
                    'mdtu-ddm-edp-cicd' : 'control-plane'
            String dockerFileSourcePath = "${context.workDir}/Dockerfile"
            String dockerFileDestPath = "${context.workDir}/Dockerfile"
            updateDockerfile(dockerFileSourcePath, context.dockerRegistry.proxyHost, globalNexusNamespace,
                    dockerFileDestPath)
        } else {
            context.script.error "There is no Dockerfile in the root directory of the project " +
                    "${context.codebase.name}. "
        }

        context.logger.debug("Get version from pom.xml")
        String version = context.script.readMavenPom(file: "pom.xml").version
        context.codebase.setImageTag(version)
        context.logger.debug("Version from pom.xml: ${version}")

        if (context.platform.checkObjectExists(BUILD_CONFIG_API, context.codebase.buildConfigName)) {
            context.logger.info("Updating build config ${context.codebase.buildConfigName}")
            context.platform.patch(BUILD_CONFIG_API, context.codebase.buildConfigName,
                    "\"{\\\"spec\\\":{\\\"output\\\":" +
                            "{\\\"to\\\":{\\\"name\\\":\\\"${context.codebase.imageUrl}\\\"}}}}\"")
        } else {
            context.logger.info("Creating build config ${context.codebase.buildConfigName}")
            context.script.sh(script: "oc new-build --name ${context.codebase.buildConfigName} " +
                    "--binary=true " +
                    "--to-docker=true " +
                    "--to=${context.codebase.imageUrl} " +
                    "--push-secret=${context.dockerRegistry.PUSH_SECRET} " +
                    "--build-arg=NEXUS_USR=${context.dockerRegistry.ciUser} " +
                    "--build-arg=NEXUS_PASS=${context.dockerRegistry.ciUserPassword}")
        }

        context.logger.info("Start build of ${context.codebase.imageUrl} image")
        String tarArchive = "${context.codebase.name}.tar"
        context.script.sh(script: "tar -cf ${tarArchive} Dockerfile src pom.xml settings.xml; " +
                "oc start-build ${context.codebase.buildConfigName} --from-archive=${tarArchive} --wait=true")

    }

    void updateDockerfile(dockerFileSourcePath, dockerProxyRegistry, NexusNamespace,
                          dockerFileDestPath) {
        String template = context.script.readFile(dockerFileSourcePath)
        context.script.writeFile(file: dockerFileDestPath, text: TemplateRenderer.renderTemplate(template,
                ["dockerProxyRegistry": dockerProxyRegistry, "globalNexusNamespace": NexusNamespace]))
    }
}
