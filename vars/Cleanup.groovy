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

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.codebase.Codebase
import com.epam.digital.data.platform.pipelines.platform.PlatformFactory
import com.epam.digital.data.platform.pipelines.registrycomponents.external.DockerRegistry
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.Gerrit
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.PostgresOperator
import com.epam.digital.data.platform.pipelines.stages.StageFactory
import com.epam.digital.data.platform.pipelines.tools.GitClient
import com.epam.digital.data.platform.pipelines.tools.Logger
import com.epam.digital.data.platform.pipelines.registry.CleanupRegistryRegulation
import jenkins.model.Jenkins

void call() {
    BuildContext context

    node("master") {
        stage("Init") {
            context = new BuildContext(this)
            String logLevel = context.getLogLevel()
            context.logger = new Logger(context.script)
            context.logger.init(logLevel)
            context.logger.info("Sucessfully inialized logger with level ${logLevel}")

            context.namespace = context.getParameterValue("CI_NAMESPACE")
            context.logger.debug("Current namespace: ${context.namespace}")

            context.deploymentMode = context.getDeploymentMode(context.namespace)
            context.logger.debug("Current deployment mode: ${context.deploymentMode}")

            context.platform = new PlatformFactory(context).getPlatformImpl()
            context.logger.debug("Current platform: ${context.platform.class.name}")

            context.logger.info("Initializing docker registry")
            context.dockerRegistry = new DockerRegistry(context)
            context.dockerRegistry.init()
            context.logger.debug(context.dockerRegistry.toString())

            context.stageFactory = new StageFactory(context)
            context.stageFactory.init()

            context.dnsWildcard = context.platform.getJsonPathValue("jenkins", "jenkins", ".spec.edpSpec.dnsWildcard")
            context.logger.debug("Current dns wildcard: ${context.dnsWildcard}")

            context.gitServer = new Gerrit(context, "gerrit")
            context.gitServer.init()
            context.logger.debug("Gitserver config: ${context.gitServer.toString()}")

            context.gitClient = new GitClient(context)

            context.logger.info("Initializing postgres")
            context.postgres = new PostgresOperator(context)
            context.postgres.init()
            context.logger.debug("PostgresOperator: ${context.postgres.toString()}")

            context.cleanup = new CleanupRegistryRegulation(context)
            String registryRegulations = "registry-regulations"
            String historyExcerptor = "history-excerptor"
            context.cleanup.recreateDefaultCodebaseRelatedResources(registryRegulations)
            context.cleanup.recreateDefaultCodebaseRelatedResources(historyExcerptor)
            context.platform.triggerDeploymentRollout("codebase-operator")
            [registryRegulations, historyExcerptor].each {
                int maxAttempts = 20
                int attempt = 0
                while (!Jenkins.getInstanceOrNull().getItemByFullName(it) ||
                        !context.platform.get("codebasebranch", "",
                                "-l affiliatedWith=$it -ojsonpath='{.items[0].metadata.finalizers}'")) {
                    attempt++
                    if (attempt == maxAttempts) {
                        context.script.error("$it resource is not connected to codebase-operator")
                    }
                    context.script.sleep(10)
                }
            }
            context.codebase = new Codebase(context)
            context.codebase.init()
            context.logger.debug("Codebase config: ${context.codebase.toString()}")

        }
    }

    node("master") {
        context.initWorkDir()
        context.codebase.initBuildTool()

        context.stageFactory.getStagesToRun().each { stagesBlock ->
            dir(context.getWorkDir()) {
                dir(context.workDir) {
                    LinkedHashMap parallelStages = [:]
                    if (stagesBlock.containsKey('parallelStages')) {
                        stagesBlock.values().each() { parallelStagesBlock ->
                            parallelStagesBlock.each { parallelStage ->
                                parallelStages["${parallelStage.name}"] = {
                                    if (parallelStage instanceof ArrayList) {
                                        parallelStage.each {
                                            context.stageFactory.runStage(it.name, context)
                                        }

                                    } else {
                                        context.stageFactory.runStage(parallelStage.name, context)
                                    }
                                }
                            }
                            context.script.parallel parallelStages
                        }
                    } else {
                        stagesBlock.values().each() { sequenceStage ->
                            sequenceStage.each {
                                context.stageFactory.runStage(it.name, context)
                            }
                        }
                    }
                }
            }
        }
    }
}
