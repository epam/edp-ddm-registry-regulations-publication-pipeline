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
import com.epam.digital.data.platform.pipelines.registrycomponents.external.Ceph
import com.epam.digital.data.platform.pipelines.registrycomponents.external.KeycloakClient
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage
import com.epam.digital.data.platform.pipelines.tools.Helm

@Stage(name = "create-excerptor-job", buildTool = "any", type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class CreateExcerptorJob {
    BuildContext context

    void run() {
        String DEPLOY_TEMPLATES_PATH = "deploy-templates"
        String JOB_YAML = "history-excerptor-job.yaml"
        Helm.upgrade(context, context.codebase.name, DEPLOY_TEMPLATES_PATH,
                ['namespace': context.namespace], "", context.namespace, true)

        context.historyExcerptor = new KeycloakClient(context)

        context.historyExcerptor.init("admin", "history-excerpt-history-user-admin",
                "keycloak-history-user-admin-client-secret")

        LinkedHashMap<String, String> parametersMap = [
                'namespace'              : context.namespace,
                'deploy'                 : "true",
                'buildId'                : context.script.env.BUILD_NUMBER,
                'dockerRegistry'         : context.dockerRegistry.proxyHost,
                'tableName'              : context.getParameterValue("NAME_OF_TABLE"),
                'DB_SCHEMA'              : context.postgres.getCurrentSchema(context.postgres.masterRepPod,
                        "registry", context.postgres.analytical_pg_user).trim(),
                'id'                     : context.getParameterValue("ID"),
                'token'                  : context.keycloak.getAccessToken(context.historyExcerptor),
                'data.historicBucketName': context.platform.getJsonPathValue(Ceph.OBJECT_BUCKET_CLAIM_API,
                        "datafactory-ceph-bucket", ".spec.bucketName"),
                'edpProject'             : context.platform.getJsonPathValue("configmap", "registry-pipeline-stage-name",
                        ".data.edpProject")
        ]

        context.script.writeFile file: JOB_YAML, text: Helm.template(context, "job", DEPLOY_TEMPLATES_PATH,
                parametersMap, context.namespace, "templates/receive-data-job.yaml")

        context.platform.apply(JOB_YAML)
    }
}
