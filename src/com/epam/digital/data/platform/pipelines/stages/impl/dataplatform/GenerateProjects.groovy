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
import com.epam.digital.data.platform.pipelines.registrycomponents.generated.DataComponent
import com.epam.digital.data.platform.pipelines.registrycomponents.generated.DataComponentType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "generate-projects", buildTool = ["any"], type = [ProjectType.LIBRARY])
class GenerateProjects {
    BuildContext context

    private final String SVC_GEN_UTIL_DIR = "/home/jenkins/service-generation-utility"

    void run() {
        String modules = ""
        context.dataComponents.values().each {
            modules += "--module=${it.name} "
        }

        context.logger.info("Generating data services")
        context.script.sh(script: "cp ${context.registry.SETTINGS_FILE} ${SVC_GEN_UTIL_DIR}")
        context.script.dir(context.getGeneratedProjectsDir()) {
            context.script.sh(script: "set +x; java -jar " +
                    "-DPOSTGRES_PASSWORD=\'${context.postgres.operational_pg_password}\' " +
                    "-DPOSTGRES_USER=${context.postgres.operational_pg_user} " +
                    "-DDB_NAME=${context.registry.name} " +
                    "-DDB_URL=${context.postgres.OPERATIONAL_MASTER_URL} " +
                    "-DDB_PORT=${context.postgres.OPERATIONAL_MASTER_PORT} " +
                    "-Dsettings=${context.getWorkDir()}/${context.registry.SETTINGS_FILE} " +
                    "-DPLATFORM_VALUES_PATH=${context.registryRegulations.getRegistryConfValues()} " +
                    "${SVC_GEN_UTIL_DIR}/service-generation-utility.jar " +
                    "${modules} " +
                    "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
        }

        context.dataComponents.values().each { dataComponent ->
            context.script.dir(dataComponent.getWorkDir()) {
                context.script.sh(script: "rm -rf ${dataComponent.DEPLOY_TEMPLATES_PATH}; " +
                        "cp -r ${context.getGeneratedProjectsDir()}/${dataComponent.name}/* .")

                def pom = context.script.readMavenPom(file: "pom.xml")
                String version = dataComponent.version
                pom.version = version
                context.script.writeMavenPom(model: pom)
                if (dataComponent.name != DataComponentType.MODEL.getValue()) {
                    String chartYamlPath = "${dataComponent.DEPLOY_TEMPLATES_PATH}/Chart.yaml"
                    LinkedHashMap chartYaml = context.script.readYaml file: chartYamlPath
                    chartYaml.appVersion = dataComponent.version
                    chartYaml.version = dataComponent.version
                    context.script.writeYaml file: chartYamlPath, data: chartYaml, overwrite: true
                    DataComponent dataModel = context.dataComponents.get(DataComponentType.MODEL.getValue())
                    def pomXml = context.script.readMavenPom(file: "pom.xml")
                    pomXml.dependencies[1].version = dataModel.version
                    context.script.writeMavenPom(model: pomXml)
                }
            }
        }
    }
}
