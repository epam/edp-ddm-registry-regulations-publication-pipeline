/*
 * Copyright 2021 EPAM Systems.
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
import com.epam.digital.data.platform.pipelines.tools.TemplateRenderer

@Stage(name = "create-trembita-business-process", buildTool = ["any"], type = ProjectType.LIBRARY)
class CreateTrembitaBusinessProcess {
    BuildContext context

    void run() {
        try {
            String BP_TREMBITA_FILE = "${RegulationType.BUSINESS_PROCESS_TREMBITA.value}/external-system.yml"
            String value = context.script.sh(script: """awk '{printf "%s%s\\n", "", \$0}' ${BP_TREMBITA_FILE}""",
                    returnStdout: true).replaceAll("\n", "\\\\n")
            boolean patched = context.platform.patchConfigMapKey(
                    "bp-webservice-gateway-trembita-business-processes", "trembita-business-processes.yml", value)
            if (patched)
                context.platform.triggerDeploymentRollout("bp-webservice-gateway")
        }

        catch (any) {
            context.logger.error("Error during uploading trembita business process changes")
            context.stageFactory.runStage(context.RESTORE_STAGE, context)
        }
        if (context.registryRegulations.filesToDeploy.get(RegulationType.BUSINESS_PROCESS_TREMBITA)) {
            try {
                String trembitaConfFile = "${RegulationType.BUSINESS_PROCESS_TREMBITA.value}/configuration.yml"
                String trembitaConfigmapKey = "trembita-configuration.yml"
                String configmapName = "trembita-configuration"
                String trembitaConfigmapYaml = "${context.script.sh(script: """awk '{printf "%s\\n", \$0}' \
                        ${trembitaConfFile}""", returnStdout: true).replaceAll("\n", "\\\\n")}"
                context.bpmsConfigMapsChanged["trembitaConfiguration"] = context.platform.patchConfigMapKey(configmapName,
                        trembitaConfigmapKey, trembitaConfigmapYaml)
                context.logger.info("Configmap trembita-configuration have been successfully updated")
            }
            catch (any) {
                context.logger.error("Error during trembita-configuration configmap updating")
            }
            String cdPipelineName = context.platform.getJsonPathValue("configmap", "registry-pipeline-stage-name",
                    ".data.cdPipelineName")
            String cdPipelineStageName = context.platform.getJsonPathValue("configmap", "registry-pipeline-stage-name",
                    ".data.cdPipelineStageName")
            LinkedHashMap trembitaYaml = context.script.readYaml file: "bp-trembita/configuration.yml"
            context.logger.info("Creating configuration from configuration.yml")
            String template = context.script.libraryResource("${context.YAML_RESOURCES_RELATIVE_PATH}" +
                    "/keycloakauthflow/citizen-portal-dso-citizen-auth-flow.yaml")
            LinkedHashMap trembitaMapPath = trembitaYaml["trembita-exchange-gateway"]["registries"]["edr-registry"]
            LinkedHashMap<String, String> binding = [
                    "namespace"              : context.namespace,
                    "trembitaUserId"         : trembitaMapPath["user-id"],
                    "registryMemberClass"    : trembitaMapPath["service"]["member-class"],
                    "clientXRoadInstance"    : trembitaMapPath["client"]["x-road-instance"],
                    "registryMemberCode"     : trembitaMapPath["service"]["member-code"],
                    "trembitaProtocolVersion": "${trembitaMapPath["protocol-version"]}",
                    "registryXRoadInstance"  : trembitaMapPath["service"]["x-road-instance"],
                    "clientSubsystemCode"    : trembitaMapPath["client"]["subsystem-code"],
                    "clientMemberCode"       : trembitaMapPath["client"]["member-code"],
                    "trembitaUrl"            : trembitaMapPath["trembita-url"],
                    "registryToken"          : trembitaMapPath["authorization-token"],
                    "registrySubsystemCode"  : trembitaMapPath["service"]["subsystem-code"],
                    "clientMemberClass"      : trembitaMapPath["client"]["member-class"],
                    "cdname"                 : "${cdPipelineName}-${cdPipelineStageName}",
                    "dnsWildcard"            : context.dnsWildcard
            ]
            String destination = "trembita-configuration.yaml"
            context.script.writeFile(file: destination, text: TemplateRenderer.renderTemplate(template, binding))
            context.platform.apply(destination)
            context.logger.info("Configuration from configuration.yml have been sucessfully created")

        } else {
            context.logger.info("Skip trembita configuration creation due to empty change list")
        }
    }
}
