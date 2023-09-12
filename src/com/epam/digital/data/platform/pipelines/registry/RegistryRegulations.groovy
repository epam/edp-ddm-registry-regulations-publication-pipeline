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

package com.epam.digital.data.platform.pipelines.registry

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class RegistryRegulations {
    private final BuildContext context

    private final String REGISTRY_REGULATIONS_CLI_JAR = "/home/jenkins/registry-regulations-cli/registry-regulations-cli.jar"

    RegistryRegulations(BuildContext context) {
        this.context = context
    }

    ArrayList<String> getAllRegulations(RegulationType regulationType) {
        context.logger.info("Get all ${regulationType.value} files")
        ArrayList<String> allRegulations
        try {
            allRegulations = context.script.sh(script: "find ${regulationType.value} " +
                    "-not -path '*/\\.*' " +
                    "-type f ! -name '*.gitkeep*' | grep '.'",
                    returnStdout: true).tokenize('\n')
            context.logger.debug(allRegulations.toString())
            return allRegulations
        } catch (any) {
            context.logger.info("No ${regulationType.value} files found")
            allRegulations = []
        }
        return allRegulations
    }

    String getRegistryConfValues(boolean getProfileValue = false) {
        if (getProfileValue) {
            return context.script.sh(script: "helm get values registry-configuration | grep 'deployProfile:' " +
                    "| awk '{print \$2}'", returnStdout: true).trim()
        } else {
            String platformValuesPath = "${context.getWorkDir()}/platform-values.yaml"
            context.script.sh(script: "helm get values registry-configuration > ${platformValuesPath}")
            return platformValuesPath
        }
    }

    ArrayList getChangedStatusOrFiles(String cliCommand, String cliParams, String cliOpt) {
        ArrayList regulationState = []
        boolean fullDeploy = context.getParameterValue("FULL_DEPLOY", "false").toBoolean()
        if (fullDeploy) {
            regulationState.add("fullDeploy")
            return regulationState
        }
        if (cliCommand == "plan") {
            try {
                regulationState = context.script.sh(script: "set +x; java -jar -DOPENSHIFT_NAMESPACE=${context.namespace} " +
                        "${REGISTRY_REGULATIONS_CLI_JAR} ${cliCommand} ${cliParams} ${cliOpt}",
                        returnStdout: true)
                        .split("PlanCommandExecutionStart")[1]
                        .split("PlanCommandExecutionEnd")[0]
                        .trim()
                        .tokenize(',')
            }
            catch (any) {
                context.script.error("Registry regulations cli failed during retrieving changes")
            }
            return regulationState
        } else {
            String regulationStateLog = context.script.sh(script: "set +x; java -jar -DOPENSHIFT_NAMESPACE=${context.namespace} " +
                    "${REGISTRY_REGULATIONS_CLI_JAR} ${cliCommand} ${cliParams} ${cliOpt}", returnStdout: true)
            return regulationStateLog
        }

    }

    boolean deployStatus(String stageName, String regulationFolderName) {
        ArrayList getdeployStatus = getChangedStatusOrFiles("plan", "${stageName}",
                "--file ${context.getWorkDir()}/${regulationFolderName}")
        if (getdeployStatus.contains("fullDeploy") || getdeployStatus.contains("true")) {
            return true
        } else {
            return false
        }

    }
}
