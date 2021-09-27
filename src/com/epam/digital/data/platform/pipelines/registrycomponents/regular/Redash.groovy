package com.epam.digital.data.platform.pipelines.registrycomponents.regular

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import groovy.json.JsonSlurperClassic

class Redash {
    private final BuildContext context

    private final String VIEWER_KEY_JSON_PATH = "viewer-api-key"
    private final String ADMIN_KEY_JSON_PATH = "admin-api-key"
    private final String REDASH_API_KEY_SECRET = "redash-api-keys"

    public String viewerUrl
    public String adminUrl

    public String viewerApiKey
    public String adminApiKey

    Redash(BuildContext context) {
        this.context = context
    }

    void init() {
        viewerUrl = "https://${context.platform.getJsonPathValue("route", "redash-viewer", ".spec.host")}"
        adminUrl = "https://${context.platform.getJsonPathValue("route", "redash-admin", ".spec.host")}"
        initApiKeys()
        viewerApiKey = context.platform.getSecretValue(REDASH_API_KEY_SECRET, VIEWER_KEY_JSON_PATH)
        adminApiKey = context.platform.getSecretValue(REDASH_API_KEY_SECRET, ADMIN_KEY_JSON_PATH)
    }

    private void initApiKeys() {
        String annotation = "is-patched"
        if (context.platform.getJsonPathValue("secret", REDASH_API_KEY_SECRET,
                ".metadata.annotations.${annotation}").toBoolean()) {
            context.logger.info("Redash api key secret is up to date")
        } else {
            context.logger.info("Redash api key secret is not yet initialized")
            String initialPassword = context.platform.getSecretValue("redash-setup-secret", "password")
            context.platform.patch("secret", REDASH_API_KEY_SECRET, "\'{\"data\": {\"${VIEWER_KEY_JSON_PATH}\": " +
                    "\"${regenerateApiKey(viewerUrl, initialPassword)}\"}}\'")
            context.platform.patch("secret", REDASH_API_KEY_SECRET, "\'{\"data\": {\"${ADMIN_KEY_JSON_PATH}\": " +
                    "\"${regenerateApiKey(adminUrl, initialPassword)}\"}}\'")
            context.platform.annotate("secret", REDASH_API_KEY_SECRET, annotation, "true", true)

            String redashExporterDeployment = "deployment/redash-exporter"
            context.platform.scale(redashExporterDeployment, 0)
            context.script.sleep(5)
            context.platform.scale(redashExporterDeployment, 1)
        }
    }

    private String regenerateApiKey(String url, String password) {
        context.logger.info("Get redash login cookie")
        def loginResponse = context.script.httpRequest url: "${url}/login",
                httpMode: "POST",
                contentType: "APPLICATION_FORM",
                requestBody: "email=user@mail.com&password=${password}",
                consoleLogResponseBody: context.logLevel == "DEBUG",
                quiet: context.logLevel != "DEBUG",
                validResponseCodes: "302"
        String cookie = loginResponse.getHeaders()
                .get("Set-Cookie")
                .toString()
                .replace("[", "")
                .replace("]", "")

        if (!cookie)
            context.script.error "Failed to get redash login cookie"

        context.logger.info("Regenerate redash api key")
        def response = context.script.httpRequest url: "${url}/api/users/1/regenerate_api_key",
                httpMode: "POST",
                customHeaders: [[name:"Cookie", value: cookie, maskValue: true]],
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
}
