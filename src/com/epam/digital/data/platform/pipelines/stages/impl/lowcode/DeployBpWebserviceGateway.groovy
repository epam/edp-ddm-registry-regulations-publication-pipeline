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

package com.epam.digital.data.platform.pipelines.stages.impl.lowcode

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage
import com.epam.digital.data.platform.pipelines.tools.Helm

@Stage(name = "deploy-bp-webservice-gateway", buildTool = ["any"], type = [ProjectType.LIBRARY])
class DeployBpWebserviceGateway {
    BuildContext context

    private final String SVC_GEN_UTIL_DIR = "/home/jenkins/service-generation-utility"
    private final String COMMIT_MESSAGE = "bp-webservice-gateway generated"

    void run() {
        String componentName = "bp-webservice-gateway"
        String DEPLOY_TEMPLATES_PATH = "deploy-templates"
        String EXTERNAL_SYSTEM_FILE = "external-system.yml"
        String modules = "--module=$componentName"
        String workDir = "${context.workDir}/$componentName"
        String generatedDir = "${context.getGeneratedProjectsDir()}-bp"
        boolean changedFile = false
        if (context.getParameterValue("FULL_DEPLOY", "false").toBoolean()) {
            changedFile = true
        } else {
            changedFile = context.registryRegulations.getChangedStatusOrFiles("plan", "deploy-bp-webservice-gateway",
                    "--file-detailed ${RegulationType.BUSINESS_PROCESS_TREMBITA.value}/$EXTERNAL_SYSTEM_FILE")
        }
        if (changedFile) {
            try {
                context.script.dir(workDir) {
                    context.script.sshagent(["${context.gitServer.credentialsId}"]) {
                        context.logger.info("Checkout ${componentName}")
                        context.gitClient.gitSetConfig()
                        context.gitClient.checkout("ssh://${context.gitServer.autouser}" +
                                "@${context.gitServer.host}:${context.gitServer.sshPort}/$componentName", "master",
                                context.gitServer.credentialsId)
                        context.script.sh(script: "git checkout master")
                        String currentCommitMessage = context.gitClient.getCurrentCommitMessage()
                        if (currentCommitMessage.contains("generated")) {
                            Helm.uninstall(context, componentName, context.namespace, true)
                            context.gitClient.gitResetHardToPreviousCommit()
                            context.gitClient.gitPushForce("master")
                        }
                    }
                }
                context.script.sh(script: "rm -rf $workDir")
                ['anyuid', 'privileged'].each { scc ->
                    context.platform.addSccToUser(componentName, scc, context.namespace)
                }
                context.logger.info("Generating $componentName")
                context.script.sh(script: "cp ${context.registry.SETTINGS_FILE} ${SVC_GEN_UTIL_DIR}")
                context.script.sh(script: "cp ${RegulationType.BUSINESS_PROCESS_TREMBITA.value}/${EXTERNAL_SYSTEM_FILE} ${SVC_GEN_UTIL_DIR}")
                context.script.dir(generatedDir) {
                    context.script.sh(script: "set +x; java -jar " +
                            "-Dsettings=${SVC_GEN_UTIL_DIR}/${context.registry.SETTINGS_FILE} " +
                            "-Ddefinitions=${SVC_GEN_UTIL_DIR}/${EXTERNAL_SYSTEM_FILE} " +
                            "-Dspring.profiles.active=db-less-mode " +
                            "${SVC_GEN_UTIL_DIR}/service-generation-utility.jar " +
                            "${modules} " +
                            "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
                }
                context.script.dir(workDir) {
                    context.logger.info("Checkout $componentName")
                    context.gitClient.checkout("ssh://${context.gitServer.autouser}" +
                            "@${context.gitServer.host}:${context.gitServer.sshPort}/$componentName", "master",
                            context.gitServer.credentialsId)
                    context.script.sh(script: "git checkout master")
                    context.script.sh(script: "rm -rf ${DEPLOY_TEMPLATES_PATH}; " +
                            "cp -r $generatedDir/$componentName/* . ")
                    def pom = context.script.readMavenPom(file: "pom.xml")
                    String version = context.codebase.version
                    pom.version = version
                    context.script.writeMavenPom(model: pom)
                    String chartYamlPath = "${DEPLOY_TEMPLATES_PATH}/Chart.yaml"
                    LinkedHashMap chartYaml = context.script.readYaml file: chartYamlPath
                    chartYaml.appVersion = context.codebase.version
                    chartYaml.version = context.codebase.version
                    context.script.writeYaml file: chartYamlPath, data: chartYaml, overwrite: true
                    context.script.sshagent(["${context.gitServer.credentialsId}"]) {
                        context.logger.info("Commiting and pushing changes to $componentName")
                        context.gitClient.gitSetConfig()
                        context.gitClient.gitAdd()
                        context.gitClient.gitCommit(COMMIT_MESSAGE)
                        context.script.retry(5) {
                            context.gitClient.gitPush('master')
                        }
                    }
                }
                context.platform.deleteObject("buildconfig", "${componentName}-master")
                context.script.build job: "${componentName}/MASTER-Build-${componentName}", wait: true, propagate: true
                LinkedHashMap<String, String> parametersMap = [
                        'namespace'          : context.namespace,
                        'ciProject'          : context.namespace,
                        'dnsWildcard'        : context.dnsWildcard,
                        'image.name'         : context.platform.getJsonPathValue("buildconfig", "${componentName}-master",
                                ".spec.output.to.name").toString().split(':')[0],
                        'image.version'      : context.codebase.version,
                        'dockerProxyRegistry': context.dockerRegistry.proxyHost,
                        'version'            : context.codebase.version,
                        'keycloak.url'       : context.keycloak.url + "/auth",
                        'deployProfile'      : context.registryRegulations.getRegistryConfValues(true),
                        'nexusPullSecret'    : context.dockerRegistry.PUSH_SECRET,
                        cdPipelineName       : context.platform.getJsonPathValue("configmap", "registry-pipeline-stage-name",
                                ".data.cdPipelineName"),
                        cdPipelineStageName  : context.platform.getJsonPathValue("configmap", "registry-pipeline-stage-name",
                                ".data.cdPipelineStageName")
                ]

                LinkedHashMap registryValues = context.script.readYaml file: context.registryRegulations.getRegistryConfValues()
                String keycloakHost = registryValues["keycloak"]["host"]
                parametersMap.put("keycloak.host", keycloakHost)
                if (context.keycloakCustomHost != null) {
                    parametersMap.put("keycloak.customHost", context.keycloakCustomHost)
                }
                Boolean requestsLimitsEnabled = registryValues["global"]["container"]["requestsLimitsEnabled"]
                if (requestsLimitsEnabled != null) {
                    parametersMap.put("global.container.requestsLimitsEnabled", requestsLimitsEnabled)
                }
                String clusterVersion = registryValues["global"]["clusterVersion"]
                if (clusterVersion != null) {
                    parametersMap.put("global.clusterVersion", clusterVersion)
                }
                if (registryValues["trembita"]["ipList"]) {
                    parametersMap.put("trembita.ipList", "\"{${registryValues["trembita"]["ipList"].join(',')}}\"")
                } else {
                    parametersMap.put("trembita.ipList", "")
                }
                String deployProfile = registryValues["deployProfile"]
                if (deployProfile != null) {
                    parametersMap.put("deployProfile", deployProfile)
                }
                String stageName = registryValues["stageName"]
                if (stageName != null) {
                    parametersMap.put("stageName", stageName)
                }
                String fileParameters = ""
                LinkedHashMap globalRegistryProperties = [global: registryValues.global]
                String componentGlobalValuesFile = "${workDir}/bpWebserviceGatewayGlobal.yaml"
                context.script.writeYaml file: componentGlobalValuesFile, data: globalRegistryProperties, overwrite: true
                fileParameters = "-f ${componentGlobalValuesFile}"
                context.script.dir(workDir) {
                    Helm.upgrade(context, componentName, DEPLOY_TEMPLATES_PATH, parametersMap, fileParameters, context.namespace)
                    context.script.sleep(10)
                    try {
                        context.logger.info("Wait up to 3min for ${componentName} become available.")
                        context.script.sh("oc wait deployment/${componentName} --for condition=available --timeout=3m")
                        context.logger.info("${componentName} is available.")
                    } catch (any) {
                        context.logger.warn("${componentName} isn't available in time! Please check manually.")
                    }
                }
                context.script.sh(script: "rm -rf $workDir $generatedDir")
                context.registryRegulations.getChangedStatusOrFiles("save", "deploy-bp-webservice-gateway",
                        "--file-detailed ${RegulationType.BUSINESS_PROCESS_TREMBITA.value}/$EXTERNAL_SYSTEM_FILE")
            } catch (any) {
                context.script.sh(script: "rm -rf $workDir $generatedDir")
                context.script.error("Something went wrong. Cleaned up.")
            }
        } else {
            context.logger.info("Skip ${RegulationType.BUSINESS_PROCESS_TREMBITA.value}/$EXTERNAL_SYSTEM_FILE deploying due to no changes")
        }
    }
}
