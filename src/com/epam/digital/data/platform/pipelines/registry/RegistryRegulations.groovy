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

package com.epam.digital.data.platform.pipelines.registry

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class RegistryRegulations {
    private final BuildContext context

    public LinkedHashMap<RegulationType, ArrayList<String>> filesToDeploy = [:]

    RegistryRegulations(BuildContext context) {
        this.context = context
    }

    ArrayList<String> getChangedRegulations(RegulationType regulationType) {
        ArrayList<String> changedRegulations
        boolean fullDeploy = context.getParameterValue("FULL_DEPLOY", "false").toBoolean()
        if (fullDeploy) {
            changedRegulations = getAllRegulations(regulationType)
        } else {
            context.logger.info("Get changed ${regulationType.value} files")
            try {
                changedRegulations = context.script.sh(script: "git diff HEAD~1 HEAD -m -1 --name-only " +
                        "--diff-filter=ACMRT " +
                        "--pretty='format:' | grep -E \"${regulationType.value}\" | grep -v .git", returnStdout: true)
                        .tokenize('\n')
                context.logger.debug(changedRegulations.toString())
            } catch (any) {
                changedRegulations = []
                context.logger.info("No changed ${regulationType.value} files found")
            }
        }
        return changedRegulations
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

    String getRegistryConfValues()
    {
        String platformValuesPath = "${context.getWorkDir()}/platform-values.yaml"
        context.script.sh(script: "helm get values registry-configuration > ${platformValuesPath}")
        return platformValuesPath
    }
}
