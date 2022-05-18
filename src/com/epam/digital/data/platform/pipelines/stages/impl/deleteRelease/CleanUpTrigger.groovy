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
        context.script.timeout(unit: 'MINUTES', time: 10) {
            LinkedHashMap parallelDeletion = [:]
            parallelDeletion["removeDataServices"] = {
                context.logger.info("Removing Data Services codebasebranches")
                context.platform.deleteObject(Codebase.CODEBASEBRANCH_CR, "-l type=data-component")
                context.platform.deleteObject(Codebase.CODEBASE_CR, "-l type=data-component")
            }
            parallelDeletion["removeRegistryRegulation"] = {
                context.logger.info("Removing registry-regulations codebasebranch codebase CR")
                context.platform.deleteObject(Codebase.CODEBASEBRANCH_CR, "${context.codebase.name}-${context.codebase.branch}")
                context.platform.deleteObject(Codebase.CODEBASE_CR, context.codebase.name)
                context.logger.info("Removing ${context.codebase.name} repo")
                context.gitServer.deleteRepository(context.codebase.name)
            }
            context.script.parallel(parallelDeletion)

            context.logger.info("Removing history-excerptor codebasebranch codebase CR")
            context.platform.deleteObject(Codebase.CODEBASEBRANCH_CR, context.codebase.historyName)
            context.platform.deleteObject(Codebase.CODEBASE_CR, context.codebase.historyName)


            [context.codebase.name, context.codebase.historyName].each {
                String tmpGerritSecret = "repository-codebase-${it}-temp"
                if (!context.platform.checkObjectExists("secret", tmpGerritSecret)) {
                    String edpGerritSecret = "edp-gerrit-ciuser"
                    context.platform.create("secret generic", tmpGerritSecret,
                            "--from-literal='username=${context.platform.getSecretValue(edpGerritSecret, "username")}' " +
                                    "--from-literal='password=${context.platform.getSecretValue(edpGerritSecret, "password")}'")
                }
            }

            context.logger.info("Creating ${context.codebase.name} codebase and codebasebranch CRs")
            ["codebase", "codebasebranch"].each {
                String emptyRepoUrl = context.codebase.sourceRepository.replaceAll(
                        context.codebase.sourceRepository.substring(
                                context.codebase.sourceRepository.lastIndexOf('/') + 1),
                        "empty-template-registry-regulation")
                String repoUrl = context.getParameterValue("RECREATE_EMPTY", "true").toBoolean()
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

            context.logger.info("Creating ${context.codebase.historyName} codebase and codebasebranch CRs")
            ["codebase", "codebasebranch"].each {
                Map binding = [
                        "repoUrl": context.codebase.sourceHistoryRepository
                ]
                String dest = "${context.codebase.historyName}-${it}.yaml"
                String template = context.script.libraryResource("${context.YAML_RESOURCES_RELATIVE_PATH}" +
                        "/${it}/${context.codebase.historyName}.yaml")
                context.script.writeFile(file: dest, text: TemplateRenderer.renderTemplate(template, binding))
                context.platform.apply(dest)
            }
        }
    }
}
