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
                            'image.version'      : context.registry.version,
                            'dockerProxyRegistry': context.dockerRegistry.proxyHost,
                            'version'            : context.registry.version,
                            'keycloak.url'       : context.keycloak.url,
                            'deployProfile'      : context.registryRegulations.getRegistryConfValues(true)
                    ]
                    LinkedHashMap platformValuesPath = context.script.readYaml file: "${context.getWorkDir()}" +
                            "/platform-values.yaml"
                    Boolean disableRequestsLimits = platformValuesPath["global"]["disableRequestsLimits"]
                    if (disableRequestsLimits != null) {
                        parametersMap.put("global.disableRequestsLimits", disableRequestsLimits)
                    }
                    context.script.dir(dataComponent.getWorkDir()) {
                        Helm.upgrade(context, dataComponent.fullName, dataComponent.DEPLOY_TEMPLATES_PATH, parametersMap,
                                context.namespace)
                    }
                }
        }
        context.script.parallel parallelDeployment
    }
}
