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
