/*
 * Copyright 2023 EPAM Systems.
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
import groovy.json.JsonSlurperClassic

class Redash {
    private final BuildContext context

    private final String VIEWER_KEY_JSON_PATH = "viewer-api-key"
    private final String ADMIN_KEY_JSON_PATH = "admin-api-key"
    private final String REDASH_API_KEY_SECRET = "redash-api-keys"
    private final String REDASH_SETUP_SECRET = "redash-system-admin-creds"

    public String viewerUrl
    public String adminUrl

    public String viewerApiKey
    public String adminApiKey

    public String deploymentMode

    LinkedHashMap redashSecretJson

    Redash(BuildContext context) {
        this.context = context
    }

    void init() {
        this.deploymentMode = context.getParameterValue("DEPLOYMENT_MODE", "development")
        viewerUrl = "http://redash-viewer/reports"
        adminUrl = "http://redash-admin/reports"
        redashSecretJson = context.platform.getAsJson("secret", REDASH_API_KEY_SECRET)["data"]
        context.script.retry(5) {
            viewerApiKey = DecodeHelper.decodeBase64(redashSecretJson[VIEWER_KEY_JSON_PATH])
            initApiKeys("viewer", VIEWER_KEY_JSON_PATH, viewerUrl, viewerApiKey)
            if (deploymentMode.equals("development")) {
                adminApiKey = DecodeHelper.decodeBase64(redashSecretJson[ADMIN_KEY_JSON_PATH])
                initApiKeys("admin", ADMIN_KEY_JSON_PATH, adminUrl, adminApiKey)
            }
        }
    }

    private void initApiKeys(String redashElement, String keyJsonPath, String redashElementUrl, String apiKey) {
        boolean isKeysRegenerated = false
        def response = context.script.httpRequest url: "${redashElementUrl}/api/users",
                httpMode: "GET",
                customHeaders: [[name: "authorization", value: apiKey, maskValue: true]],
                consoleLogResponseBody: context.logLevel == "DEBUG",
                quiet: context.logLevel != "DEBUG",
                validResponseCodes: "200,403,404"
        context.logger.debug("Redash ${redashElement} response: ${response.content}")

        if (response.getStatus() == 404) {
            context.logger.info("Redash ${redashElement} api key is no more valid or not yet initialised")
            if ("${redashElement}" == "viewer") {
                viewerApiKey = DecodeHelper.decodeBase64(patchRedashSecret(redashElementUrl, REDASH_SETUP_SECRET, keyJsonPath))
            }
            if ("${redashElement}" == "admin") {
                adminApiKey = DecodeHelper.decodeBase64(patchRedashSecret(redashElementUrl, REDASH_SETUP_SECRET, keyJsonPath))
            }
            isKeysRegenerated = true
        } else {
            context.logger.info("Redash api key secret is up to date for ${redashElement}")
        }
        if (isKeysRegenerated)
            restartRedashExporterPod()
        if (response.getStatus() == 403) {
            String systemAdminEmail = context.platform.getSecretValue(REDASH_SETUP_SECRET, "email")
            context.script.error("Please ensure that redash system admin is added to admin group in redash.\n" +
                    "Possible solution:\n" +
                    "1. Check if there is no user with email $systemAdminEmail in Keycloak;\n" +
                    "2. Run the following commands in redash deployment pod(s):\n" +
                    "export DATABASE_URL=postgresql://\$REDASH_DATABASE_USER:\$REDASH_DATABASE_PASSWORD@\$REDASH_DATABASE_HOSTNAME/\$REDASH_DATABASE_DB;\n" +
                    "python ./manage.py users grant_admin $systemAdminEmail")
        }
    }

    private String regenerateApiKey(String url, String password) {
        context.logger.info("Get redash login cookie")
        String systemAdminEmail = context.platform.getSecretValue(REDASH_SETUP_SECRET, "email")
        def loginResponse = context.script.httpRequest url: "${url}/login",
                httpMode: "POST",
                contentType: "APPLICATION_FORM",
                requestBody: "email=${systemAdminEmail}&password=${password}",
                consoleLogResponseBody: context.logLevel == "DEBUG",
                quiet: context.logLevel != "DEBUG",
                validResponseCodes: "302"
        ArrayList cookies = loginResponse.getHeaders()
                .get("Set-Cookie")
                .toString()
                .replace("[", "")
                .replace("]", "")
                .split("Path=/, ")
        String cookie = cookies.find { it.contains("session") }

        if (!cookie)
            context.script.error "Failed to get redash login cookie"

        context.logger.info("Regenerate redash api key")
        def response = context.script.httpRequest url: "${url}/api/users/1/regenerate_api_key",
                httpMode: "POST",
                customHeaders: [[name: "Cookie", value: cookie, maskValue: true]],
                consoleLogResponseBody: context.logLevel == "DEBUG",
                quiet: context.logLevel != "DEBUG",
                validResponseCodes: "200"

        return new JsonSlurperClassic()
                .parseText(response.content)
                .api_key
                .bytes
                .encodeBase64()
                .toString()
    }

    void restartRedashExporterPod() {
        String redashExporterDeployment = "deployment/redash-exporter"
        context.platform.scale(redashExporterDeployment, 0)
        context.script.sleep(5)
        context.platform.scale(redashExporterDeployment, 1)
    }

    def patchRedashSecret(String url, String secretName, String keyJsonPath) {
        String initialPassword = context.platform.getSecretValue(secretName, "password")
        def newApiKey = regenerateApiKey(url, initialPassword)
        context.platform.patch("secret", REDASH_API_KEY_SECRET, "\'{\"data\": {\"${keyJsonPath}\": " +
                "\"${newApiKey}\"}}\'")
        return newApiKey
    }

    void deleteRedashResource(String url, String apiKey) {
        def content = getContent(url, apiKey)
        def parsedJson = new JsonSlurperClassic().parseText(content)
        if (url.contains("api/dashboards")) {
            removeDashboards(parsedJson, url, apiKey)
            def defaultPageSize = parsedJson.results.size()
            def countLeftToDelete = parsedJson.count - defaultPageSize
            def numberOfIterations = countLeftToDelete > 0 ? Math.ceil(countLeftToDelete / defaultPageSize) : 0
            numberOfIterations.times {
                content = getContent(url, apiKey)
                parsedJson = new JsonSlurperClassic().parseText(content)
                removeDashboards(parsedJson, url, apiKey)
            }
        } else {
            int resourcesCount = parsedJson.size()
            resourcesCount.times {
                String nameValue
                String id
                nameValue = parsedJson[it]["name"]
                if (!nameValue.matches("(.*)audit(.*)") && !nameValue.matches("admin") &&
                        !nameValue.matches("default")) {
                    id = parsedJson[it]["id"]
                    context.logger.info("Removing not audit resource: " + nameValue)
                    context.script.httpRequest url: "${url}/${id}",
                            httpMode: "DELETE",
                            customHeaders: [[name: "authorization", value: apiKey, maskValue: true]],
                            consoleLogResponseBody: context.logLevel == "DEBUG",
                            quiet: context.logLevel != "DEBUG",
                            validResponseCodes: "200,204"
                }
            }
        }
    }

    private String getContent(String url, String apiKey) {
        def response = context.script.httpRequest url: "${url}",
                httpMode: "GET",
                customHeaders: [[name: "authorization", value: apiKey, maskValue: true]],
                consoleLogResponseBody: context.logLevel == "DEBUG",
                quiet: context.logLevel != "DEBUG",
                validResponseCodes: "200"
        return response.content
    }

    private void removeDashboards(Object parsedJsonResponse, String url, String apiKey) {
        parsedJsonResponse.results.each { dashboard ->
            context.logger.info("Removing dashboard: ${dashboard.name}")
            context.script.httpRequest url: "${url}/${dashboard.id}",
                    httpMode: "DELETE",
                    customHeaders: [[name: "authorization", value: apiKey, maskValue: true]],
                    consoleLogResponseBody: context.logLevel == "DEBUG",
                    quiet: context.logLevel != "DEBUG",
                    validResponseCodes: "200,204"
        }
    }
}