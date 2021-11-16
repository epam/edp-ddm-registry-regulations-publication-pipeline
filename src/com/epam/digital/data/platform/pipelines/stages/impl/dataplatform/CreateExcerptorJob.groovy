package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registrycomponents.external.Ceph
import com.epam.digital.data.platform.pipelines.registrycomponents.external.KeycloakClient
import com.epam.digital.data.platform.pipelines.stages.Stage
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.tools.Helm

@Stage(name = "create-excerptor-job", buildTool = "any", type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class CreateExcerptorJob {
    BuildContext context

    void run() {
        String DEPLOY_TEMPLATES_PATH = "deploy-templates"
        String JOB_YAML = "history-excerptor-job.yaml"
        Helm.upgrade(context, context.codebase.name, DEPLOY_TEMPLATES_PATH,
                ['namespace': context.namespace], context.namespace, true)

        context.historyExcerptor = new KeycloakClient(context)

        context.historyExcerptor.init("admin", "history-excerpt-history-user-admin",
                "keycloak-history-user-admin-client-secret")

        LinkedHashMap<String, String> parametersMap = [
                'namespace'              : context.namespace,
                'deploy'                 : "true",
                'buildId'                : context.script.env.BUILD_NUMBER,
                'dockerRegistry'         : context.dockerRegistry.proxyHost,
                'tableName'              : context.getParameterValue("NAME_OF_TABLE"),
                'DB_SCHEMA'              : context.citus.getCurrentSchema(context.citus.masterRepPod,
                        "registry").trim(),
                'id'                     : context.getParameterValue("ID"),
                'token'                  : context.keycloak.getAccessToken(context.historyExcerptor),
                'data.requestBucketName' : context.platform.getJsonPathValue(Ceph.OBJECT_BUCKET_CLAIM_API,
                        "lowcode-form-data-storage", ".spec.bucketName"),
                'data.historicBucketName': context.platform.getJsonPathValue(Ceph.OBJECT_BUCKET_CLAIM_API,
                        "datafactory-ceph-bucket", ".spec.bucketName")
        ]

        context.script.writeFile file: JOB_YAML, text: Helm.template(context, "job", DEPLOY_TEMPLATES_PATH,
                parametersMap, context.namespace, "templates/receive-data-job.yaml")

        context.platform.apply(JOB_YAML)
    }
}
