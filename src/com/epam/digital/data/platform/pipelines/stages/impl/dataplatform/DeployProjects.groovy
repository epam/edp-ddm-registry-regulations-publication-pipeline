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

package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registrycomponents.generated.DataComponentType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage
import com.epam.digital.data.platform.pipelines.tools.Helm

@Stage(name = "deploy-projects", buildTool = ["any"], type = [ProjectType.LIBRARY])
class DeployProjects {
    BuildContext context

    void run() {
        context.dataComponents.values().each { dataComponent ->
            if (dataComponent.name != DataComponentType.MODEL.getValue())
                ['anyuid', 'privileged'].each { scc ->
                    context.platform.addSccToUser(dataComponent.fullName, scc, context.namespace)
                }
        }

        LinkedHashMap parallelDeployment = [:]
        context.dataComponents.values().each { dataComponent ->
            if (dataComponent.name != DataComponentType.MODEL.getValue())
                parallelDeployment["${dataComponent.name}"] = {
                    context.logger.info("Deploying ${dataComponent.name}")
                    LinkedHashMap<String, String> parametersMap = [
                            'namespace'          : context.namespace,
                            'ciProject'          : context.namespace,
                            'dnsWildcard'        : context.dnsWildcard,
                            'image.name'         : context.platform.getJsonPathValue("buildconfig",
                                    ("${dataComponent.codebaseName}-${dataComponent.codebaseBranch}").replace('.', '-'),
                                    ".spec.output.to.name").toString().split(':')[0],
                            'image.version'      : dataComponent.version,
                            'dockerProxyRegistry': context.dockerRegistry.proxyHost,
                            'version'            : dataComponent.version,
                            'keycloak.url'       : context.keycloak.url + "/auth",
                            'deployProfile'      : context.registryRegulations.getRegistryConfValues(true),
                            'nexusPullSecret'    : context.dockerRegistry.PUSH_SECRET,
                            'pipelineStageName'  : context.platform.getJsonPathValue("configmap", "registry-pipeline-stage-name",
                                    ".data.cdPipelineName") + '-' + context.platform.getJsonPathValue("configmap", "registry-pipeline-stage-name",
                                    ".data.cdPipelineStageName")
                    ]
                    LinkedHashMap platformValuesPath = context.script.readYaml file: "${context.getWorkDir()}" +
                            "/platform-values.yaml"
                    Boolean requestsLimitsEnabled = platformValuesPath["global"]["container"]["requestsLimitsEnabled"]
                    if (requestsLimitsEnabled != null) {
                        parametersMap.put("global.container.requestsLimitsEnabled", requestsLimitsEnabled)
                    }

                    String deploymentMode = platformValuesPath["global"]["deploymentMode"]
                    if (deploymentMode != null) {
                        parametersMap.put("global.deploymentMode", deploymentMode)
                    }

                    if (context.keycloakCustomHost != null) {
                        parametersMap.put("keycloak.customHost", context.keycloakCustomHost)
                    }

                    String clusterVersion = platformValuesPath["global"]["clusterVersion"]
                    if (clusterVersion != null) {
                        parametersMap.put("global.clusterVersion", clusterVersion)
                    }

                    if (dataComponent.name == DataComponentType.REST_API.getValue()) {
                        String kafkaRequestReplyTimeout = platformValuesPath["global"]["registry"]["restApi"]["kafka"]?.timeoutInSeconds
                        if (kafkaRequestReplyTimeout != null) {
                            parametersMap.put("global.registry.restApi.kafka.timeoutInSeconds", kafkaRequestReplyTimeout)
                        }
                    }

                    if (dataComponent.name == DataComponentType.SOAP_API.getValue()) {
                        context.logger.info("Add trembita IPs to SOAP api")
                        LinkedHashMap registryValues = context.script.readYaml text: context.script.sh(script: """helm get values registry-nodes -a -n ${context.namespace}""", returnStdout: true)
                        if (registryValues["trembita"]["ipList"])
                            parametersMap.put("trembita.ipList", "\"{${registryValues["trembita"]["ipList"].join(',')}}\"")
                        context.logger.info("Parameters: ${parametersMap}")
                    }

                    String fileParameters = ""
                    String dataComponentNameNormalized = dataComponent.name.replace('-','').replace('api',"Api")
                    LinkedHashMap globalRegistryProperties = [global: platformValuesPath.global]
                    String componentGlobalValuesFile = "${dataComponent.getWorkDir()}/${dataComponentNameNormalized}Global.yaml"
                    context.script.writeYaml file: componentGlobalValuesFile, data: globalRegistryProperties, overwrite: true
                    fileParameters = "-f ${componentGlobalValuesFile}"

                    context.script.dir(dataComponent.getWorkDir()) {
                        Helm.upgrade(context, dataComponent.fullName, dataComponent.DEPLOY_TEMPLATES_PATH, parametersMap,
                                fileParameters, context.namespace)

                        context.script.sleep(10)
                        try {
                            context.logger.info("Wait up to 10min for ${dataComponent.name} become available.")
                            context.script.sh("oc wait deployment/registry-${dataComponent.name}-deployment --for condition=available --timeout=10m")
                            context.logger.info("${dataComponent.name} is available.")
                        } catch (any) {
                            context.logger.warn("${dataComponent.name} isn't available in time! Please check manually.")
                        }
                    } 
                }
        }
        context.script.parallel parallelDeployment
    }
}
