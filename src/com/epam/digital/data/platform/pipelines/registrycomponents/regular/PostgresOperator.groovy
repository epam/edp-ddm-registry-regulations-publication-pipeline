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

package com.epam.digital.data.platform.pipelines.registrycomponents.regular

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.helper.DecodeHelper

class PostgresOperator {
    private final BuildContext context

    public final static String OPERATIONAL_MASTER_URL = "operational-primary"
    public final static String OPERATIONAL_MASTER_PORT = 5432
    public final static String ANALYTICAL_MASTER_URL = "analytical-primary"
    public final static String ANALYTICAL_MASTER_PORT = 5432
    public final static String OPERATIONAL_CLUSTER_SECRET = "operational-pguser-postgres"
    public final static String ANALYTICAL_CLUSTER_SECRET = "analytical-pguser-postgres"
    public final static String POSTGRES_ROLES_SECRET = "citus-roles-secrets"

    public String operational_pg_user
    public String operational_pg_password
    public String analytical_pg_user
    public String analytical_pg_password
    public String auditServiceUsername
    public String auditServicePassword
    public String adminRole
    public String adminRolePass
    public String auditRolePass
    public String excerptExporterUser
    public String excerptExporterPass
    public String ownerRole
    public String ownerRolePass
    public String appRole
    public String appRolePass
    public String analyticsAdminRolePass
    public String notificationPublisherUser
    public String notificationPublisherPass
    public String geoServerPublisherUser
    public String geoServerPublisherPass

    public String masterPod
    public String masterRepPod

    LinkedHashMap operationalClusterSecretJson
    LinkedHashMap analyticalClusterSecretJson
    LinkedHashMap postgresSecretRolesJson

    PostgresOperator(BuildContext context) {
        this.context = context
    }
    private final Script script
    void init() {

        operationalClusterSecretJson = context.platform.getAsJson("secret", OPERATIONAL_CLUSTER_SECRET)["data"]
        analyticalClusterSecretJson = context.platform.getAsJson("secret", ANALYTICAL_CLUSTER_SECRET)["data"]
        operational_pg_user = DecodeHelper.decodeBase64(operationalClusterSecretJson["user"])
        operational_pg_password = DecodeHelper.decodeBase64(operationalClusterSecretJson["password"])
        analytical_pg_user = DecodeHelper.decodeBase64(analyticalClusterSecretJson["user"])
        analytical_pg_password = DecodeHelper.decodeBase64(analyticalClusterSecretJson["password"])

        postgresSecretRolesJson = context.platform.getAsJson("secret", POSTGRES_ROLES_SECRET)["data"]
        auditServiceUsername = DecodeHelper.decodeBase64(postgresSecretRolesJson["anSvcName"])
        auditServicePassword = DecodeHelper.decodeBase64(postgresSecretRolesJson["anSvcPass"])
        adminRole = DecodeHelper.decodeBase64(postgresSecretRolesJson["admRoleName"])
        adminRolePass = DecodeHelper.decodeBase64(postgresSecretRolesJson["admRolePass"])
        ownerRole = DecodeHelper.decodeBase64(postgresSecretRolesJson["regOwnerName"])
        ownerRolePass = DecodeHelper.decodeBase64(postgresSecretRolesJson["regOwnerPass"])
        appRole = DecodeHelper.decodeBase64(postgresSecretRolesJson["appRoleName"])
        appRolePass = DecodeHelper.decodeBase64(postgresSecretRolesJson["appRolePass"])
        auditRolePass = DecodeHelper.decodeBase64(postgresSecretRolesJson["anRolePass"])
        excerptExporterUser = DecodeHelper.decodeBase64(postgresSecretRolesJson["excerptExporterName"])
        excerptExporterPass = DecodeHelper.decodeBase64(postgresSecretRolesJson["excerptExporterPass"])
        analyticsAdminRolePass = DecodeHelper.decodeBase64(postgresSecretRolesJson["anAdmPass"])
        notificationPublisherUser = DecodeHelper.decodeBase64(postgresSecretRolesJson["notificationTemplatePublisherName"])
        notificationPublisherPass = DecodeHelper.decodeBase64(postgresSecretRolesJson["notificationTemplatePublisherPass"])
        geoServerPublisherUser = DecodeHelper.decodeBase64(postgresSecretRolesJson["geoserverRoleName"])
        geoServerPublisherPass = DecodeHelper.decodeBase64(postgresSecretRolesJson["geoserverRolePass"])

        masterPod = context.platform.getAll("pods", "-l postgres-operator.crunchydata.com/role=master," +
                "postgres-operator.crunchydata.com/cluster=operational -o jsonpath=\"{.items[0].metadata.name}\"")
        masterRepPod = context.platform.getAll("pods", "-l postgres-operator.crunchydata.com/role=master," +
                "postgres-operator.crunchydata.com/cluster=analytical -o jsonpath=\"{.items[0].metadata.name}\"")
    }

    String psqlCommand(String pod, String command, String database = "", String user) {
        try {
            context.platform.podExec(pod, "psql ${database} -U ${user} -t -c \"${command}\"")
        } catch (any) {
            context.script.error("Failed to execute command ${command} on ${pod}")
        }
    }

    void psqlScript(String pod, String script, String user, String options = "") {
        context.platform.podExec(pod, "psql -U ${user} -f ${script} ${options}")
    }

    void getCurrentSchema(String pod, String database = "", String user) {
        context.platform.podExec(pod, "psql ${database} -U ${user} -t -c \"select current_schema();\"")
    }

    @Override
    String toString() {
        return "PostgresOperator{" +
                "masterPod='" + masterPod + '\'' +
                ", masterRepPod='" + masterRepPod + '\'' +
                '}'
    }
}
