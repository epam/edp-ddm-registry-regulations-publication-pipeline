package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registrycomponents.external.Ceph
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
                        'namespace': context.namespace,
                        'ciProject': context.namespace,
                        'dnsWildcard': context.dnsWildcard,
                        'image.name': "${context.dockerRegistry.host}/${context.namespace}/" +
                                "${dataComponent.codebaseName}-${dataComponent.codebaseBranch}",
                        'image.version': context.registry.version,
                        'version': context.registry.version,
                        'keycloak.url': context.keycloak.url,
                        'ceph.httpEndpoint': Ceph.CEPH_INTERNAL_ENDPOINT,
                        'datafactoryResponseCeph.httpEndpoint': Ceph.CEPH_INTERNAL_ENDPOINT,
                        'datafactoryceph.httpEndpoint': Ceph.CEPH_INTERNAL_ENDPOINT,
                        'datafactoryFileCeph.httpEndpoint': Ceph.CEPH_INTERNAL_ENDPOINT,
                        'lowcodeFileCeph.httpEndpoint': Ceph.CEPH_INTERNAL_ENDPOINT
                ]

                context.script.dir(dataComponent.getWorkDir()) {
                    Helm.upgrade(context, dataComponent.codebaseName, dataComponent.DEPLOY_TEMPLATES_PATH, parametersMap,
                            context.namespace)
                }
            }
        }
        context.script.parallel parallelDeployment
    }
}
