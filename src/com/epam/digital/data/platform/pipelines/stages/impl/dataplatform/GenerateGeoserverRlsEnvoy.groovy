package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage
import com.epam.digital.data.platform.pipelines.tools.Helm

@Stage(name = "generate-geoserver-rls-envoy", buildTool = ["any"], type = [ProjectType.LIBRARY])
class GenerateGeoserverRlsEnvoy {
  BuildContext context

  private final String SVC_GEN_UTIL_DIR = "/home/jenkins/service-generation-utility"

  void run() {
    String DEPLOY_TEMPLATES_PATH = "geoserver-rls/deploy-templates"
    String modules = "--module=geoserver-rls"

    context.logger.info("Generating geoserver rls filter")
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

      LinkedHashMap platformValuesPath = context.script.readYaml file: "${context.getWorkDir()}" +
              "/platform-values.yaml"
      LinkedHashMap<String, String> parametersMap = [
              'namespace'          : context.namespace,
              'keycloak.host'      : platformValuesPath["keycloak"]["host"]
      ]

      Helm.upgrade(context, "registry-geoserver-rls", DEPLOY_TEMPLATES_PATH,
              parametersMap, "", context.namespace, true)
    }

  }
}
