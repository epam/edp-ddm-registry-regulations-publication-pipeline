package com.epam.digital.data.platform.pipelines.stages.impl.deleteRelease

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.codebase.Codebase
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage
import com.epam.digital.data.platform.pipelines.tools.TemplateRenderer

@Stage(name = "cleanup-trigger", buildTool = ["any"], type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class CleanUpTrigger {
    BuildContext context

    void run() {
        context.logger.info("Removing Data Services codebasebranches")
        context.dataComponents.values().each {
            context.platform.deleteObject(Codebase.CODEBASEBRANCH_CR, "-l type=data-component " +
                    "-l version=${context.registry.version}")
            context.platform.deleteObject(Codebase.CODEBASE_CR, it.codebaseName)
        }

        context.logger.info("Removing registry-regulations codebasebranch codebase CR")
        context.platform.deleteObject(Codebase.CODEBASEBRANCH_CR, "${context.codebase.name}-${context.codebase.branch}")
        context.platform.deleteObject(Codebase.CODEBASE_CR, context.codebase.name)
        context.logger.info("Removing ${context.codebase.name} repo")
        context.gitServer.deleteRepository(context.codebase.name)

        String tmpGerritSecret = "repository-codebase-${context.codebase.name}-temp"
        if (!context.platform.checkObjectExists("secret", tmpGerritSecret)) {
            String edpGerritSecret = "edp-gerrit-ciuser"
            context.platform.create("secret generic", tmpGerritSecret,
                    "--from-literal='username=${context.platform.getSecretValue(edpGerritSecret, "username")}' " +
                            "--from-literal='password=${context.platform.getSecretValue(edpGerritSecret, "password")}'")
        }

        context.logger.info("Creating ${context.codebase.name} codebase and codebasebranch CRs")
        ["codebase", "codebasebranch"].each {
            String emptyRepoUrl = context.codebase.sourceRepository.replaceAll(
                    context.codebase.sourceRepository.substring(
                            context.codebase.sourceRepository.lastIndexOf('/') + 1),
                    "empty-template-registry-regulation")
            String repoUrl =  context.getParameterValue("RECREATE_EMPTY", "true").toBoolean()
                    ? emptyRepoUrl : context.codebase.sourceRepository
            Map binding = [
                    "repoUrl"       : repoUrl,
                    "codebaseName"  : context.codebase.name,
                    "branch"        : context.codebase.branch,
                    "defaultBranch" : context.codebase.branch,
                    "version"       : context.registry.version,
                    "buildTool"     : context.codebase.buildToolSpec,
                    "jenkinsAgent"  : context.codebase.jenkinsAgent,
                    "jobProvisioner": context.codebase.jobProvisioner
            ]
            String dest = "${context.codebase.name}-${it}.yaml"
            String template = context.script.libraryResource("${context.YAML_RESOURCES_RELATIVE_PATH}" +
                    "/${it}/${context.codebase.name}.yaml")
            context.script.writeFile(file: dest, text: TemplateRenderer.renderTemplate(template, binding))
            context.platform.apply(dest)
        }
    }
}
