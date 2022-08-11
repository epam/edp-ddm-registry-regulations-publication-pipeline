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

class Citus {
    private final BuildContext context

    public final static String CITUS_MASTER_URL = "citus-master"
    public final static String CITUS_MASTER_PORT = 5432
    public final static String CITUS_MASTER_REP_URL = "citus-master-rep"
    public final static String CITUS_MASTER_REP_PORT = 5432
    public final static String CITUS_SECRET = "citus-secrets"
    public final static String CITUS_ROLES_SECRET = "citus-roles-secrets"

    public String user
    public String password
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
    public ArrayList<String> workersPods
    public ArrayList<String> workersRepPods

    LinkedHashMap citusSecretJson
    LinkedHashMap citusSecretRolesJson

    Citus(BuildContext context) {
        this.context = context
    }

    void init() {

        citusSecretJson = context.platform.getAsJson("secret", CITUS_SECRET)["data"]
        user = DecodeHelper.decodeBase64(citusSecretJson["username"])
        password = DecodeHelper.decodeBase64(citusSecretJson["password"])

        citusSecretRolesJson = context.platform.getAsJson("secret", CITUS_ROLES_SECRET)["data"]
        auditServiceUsername = DecodeHelper.decodeBase64(citusSecretRolesJson["anSvcName"])
        auditServicePassword = DecodeHelper.decodeBase64(citusSecretRolesJson["anSvcPass"])
        adminRole = DecodeHelper.decodeBase64(citusSecretRolesJson["admRoleName"])
        adminRolePass = DecodeHelper.decodeBase64(citusSecretRolesJson["admRolePass"])
        ownerRole = DecodeHelper.decodeBase64(citusSecretRolesJson["regOwnerName"])
        ownerRolePass = DecodeHelper.decodeBase64(citusSecretRolesJson["regOwnerPass"])
        appRole = DecodeHelper.decodeBase64(citusSecretRolesJson["appRoleName"])
        appRolePass = DecodeHelper.decodeBase64(citusSecretRolesJson["appRolePass"])
        auditRolePass = DecodeHelper.decodeBase64(citusSecretRolesJson["anRolePass"])
        excerptExporterUser = DecodeHelper.decodeBase64(citusSecretRolesJson["excerptExporterName"])
        excerptExporterPass = DecodeHelper.decodeBase64(citusSecretRolesJson["excerptExporterPass"])
        analyticsAdminRolePass = DecodeHelper.decodeBase64(citusSecretRolesJson["anAdmPass"])
        notificationPublisherUser = DecodeHelper.decodeBase64(citusSecretRolesJson["notificationTemplatePublisherName"])
        notificationPublisherPass = DecodeHelper.decodeBase64(citusSecretRolesJson["notificationTemplatePublisherPass"])
        geoServerPublisherUser = DecodeHelper.decodeBase64(citusSecretRolesJson["geoserverRoleName"])
        geoServerPublisherPass = DecodeHelper.decodeBase64(citusSecretRolesJson["geoserverRolePass"])

        masterPod = context.platform.getAll("pods", "-l app=citus-master " +
                "-o jsonpath=\"{.items[0].metadata.name}\"")
        masterRepPod = context.platform.getAll("pods", "-l app=citus-master-rep " +
                "-o jsonpath=\"{.items[0].metadata.name}\"")
        workersPods = context.platform.getAll("pods", "-l app=citus-workers " +
                "-o jsonpath=\"{.items[*].metadata.name}\"").tokenize()
        workersRepPods = context.platform.getAll("pods", "-l app=citus-workers-rep " +
                "-o jsonpath=\"{.items[*].metadata.name}\"").tokenize()
    }

    String psqlCommand(String pod, String command, String database = "") {
        try {
            context.platform.podExec(pod, "psql ${database} -U ${user} -t -c \"${command}\"")
        } catch (any) {
            context.script.error("Failed to execute command ${command} on ${pod}")
        }
    }

    void psqlScript(String pod, String script, String options = "") {
        context.platform.podExec(pod, "psql -U ${user} -f ${script} ${options}")
    }

    void getCurrentSchema(String pod, String database = "") {
        context.platform.podExec(pod, "psql ${database} -U ${user} -t -c \"select current_schema();\"")
    }

    @Override
    String toString() {
        return "Citus{" +
                "masterPod='" + masterPod + '\'' +
                ", masterRepPod='" + masterRepPod + '\'' +
                ", citusWorkersPods=" + workersPods +
                ", citusWorkersRepPods=" + workersRepPods +
                '}'
    }
}
