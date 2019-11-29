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

package com.epam.digital.data.platform.pipelines.stages.impl.general

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.Registry
import com.epam.digital.data.platform.pipelines.registry.RegistryRegulations
import com.epam.digital.data.platform.pipelines.registrycomponents.generated.DataComponent
import com.epam.digital.data.platform.pipelines.registrycomponents.generated.DataComponentType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "init-registry", buildTool = ["any"], type = [ProjectType.LIBRARY])
class InitRegistry {
    BuildContext context

    void run() {
        context.logger.info("Initializing registry and data components")
        context.registry = new Registry(context)
        context.registry.init()
        context.logger.debug("Initialized registry: ${context.registry.toString()}")
        context.registryRegulations = new RegistryRegulations(context)
        context.dataComponents = [:]
        DataComponentType.values().each {
            DataComponent dataComponent = new DataComponent(context, it.getValue(), context.registry.name,
                    context.registry.version, context.codebase.branch, "ssh://${context.gitServer.autouser}" +
                    "@${context.gitServer.host}:${context.gitServer.sshPort}/${context.registry.name}-" +
                    "${it.getValue()}-${context.registry.version}",
                    context.codebase.jobProvisioner, context.codebase.jenkinsAgent)
            context.dataComponents.put(it.getValue(), dataComponent)
            context.logger.debug("Initialized data component: ${dataComponent.toString()}")
        }
        /* Remove soap-api if there is no tag "exposeSearchCondition" in data-model */
        if (context.script.sh(script: "grep -iRw exposeSearchCondition data-model/ || true", returnStdout: true).trim() == "")
            context.dataComponents.remove(DataComponentType.SOAP_API.getValue())
        context.logger.info("Registry and registry data components have been successfully initialized")
    }
}
