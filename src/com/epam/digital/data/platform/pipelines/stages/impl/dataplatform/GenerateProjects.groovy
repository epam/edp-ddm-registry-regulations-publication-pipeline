package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
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
            context.script.sh(script: "java -jar " +
                    "-DPOSTGRES_PASSWORD=${context.citus.password} " +
                    "-DPOSTGRES_USER=${context.citus.user} " +
                    "-DDB_NAME=${context.registry.name} " +
                    "-DDB_URL=${context.citus.CITUS_MASTER_URL} " +
                    "-DDB_PORT=${context.citus.CITUS_MASTER_PORT} " +
                    "-Dsettings=${context.getWorkDir()}/${context.registry.SETTINGS_FILE} " +
                    "${SVC_GEN_UTIL_DIR}/service-generation-utility.jar " +
                    "${modules} " +
                    "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
        }

        context.dataComponents.values().each { dataComponent ->
            context.script.dir(dataComponent.getWorkDir()) {
                context.script.sh(script: "rm -rf ${dataComponent.DEPLOY_TEMPLATES_PATH}; " +
                        "cp -r ${context.getGeneratedProjectsDir()}/${dataComponent.name}/* .")
                if (dataComponent.name != DataComponentType.MODEL.getValue()) {
                    String chartYamlPath = "${dataComponent.DEPLOY_TEMPLATES_PATH}/Chart.yaml"
                    LinkedHashMap chartYaml = context.script.readYaml file: chartYamlPath
                    chartYaml.appVersion = context.registry.version
                    chartYaml.version = context.registry.version
                    context.script.writeYaml file: chartYamlPath, data: chartYaml, overwrite: true
                }
            }
        }
    }
}
