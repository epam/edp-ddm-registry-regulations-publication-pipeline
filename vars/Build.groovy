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
import com.epam.digital.data.platform.pipelines.registrycomponents.external.Keycloak
import com.epam.digital.data.platform.pipelines.registrycomponents.external.KeycloakClient
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.Citus
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.Gerrit
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.Redash
import com.epam.digital.data.platform.pipelines.stages.StageFactory
import com.epam.digital.data.platform.pipelines.tools.GitClient
import com.epam.digital.data.platform.pipelines.tools.Logger

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

            context.platform = new PlatformFactory(context).getPlatformImpl()
            context.logger.debug("Current platform: ${context.platform.class.name}")

            context.logger.info("Initializing docker registry")
            context.dockerRegistry = new DockerRegistry(context)
            context.dockerRegistry.init()
            context.logger.debug(context.dockerRegistry.toString())

            context.codebase = new Codebase(context)
            context.codebase.init()
            context.logger.debug("Codebase config: ${context.codebase.toString()}")

            context.stageFactory = new StageFactory(context)
            context.stageFactory.init()

            context.dnsWildcard = context.platform.getJsonPathValue("jenkins", "jenkins", ".spec.edpSpec.dnsWildcard")
            context.logger.debug("Current dns wildcard: ${context.dnsWildcard}")

            context.gitServer = new Gerrit(context, "gerrit")
            context.gitServer.init()
            context.logger.debug("Gitserver config: ${context.gitServer.toString()}")

            context.gitClient = new GitClient(context)

            context.logger.info("Initializing citus")
            context.citus = new Citus(context)
            context.citus.init()
            context.logger.debug("Citus: ${context.citus.toString()}")

            context.logger.info("Initializing redash")
            context.redash = new Redash(context)
            context.redash.init()

            context.logger.info("Initializing keycloak")
            context.keycloak = new Keycloak(context)
            context.keycloak.init()
            context.logger.debug("Initialized Keycloak: ${context.keycloak.toString()}")
            context.jenkinsDeployer = new KeycloakClient(context)
            context.jenkinsDeployer.init("admin", "jenkins-deployer", "jenkins-keycloak-client")
        }
    }

    node(context.codebase.jenkinsAgent) {
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
