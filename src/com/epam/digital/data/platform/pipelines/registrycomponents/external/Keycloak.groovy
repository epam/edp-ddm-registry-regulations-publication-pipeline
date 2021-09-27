package com.epam.digital.data.platform.pipelines.registrycomponents.external

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import groovy.json.JsonSlurperClassic

class Keycloak {
    private final BuildContext context

    private final String DEPLOYER_REALM_CR = "admin"
    private final String DEPLOYER_CLIENT_CR = "jenkins-deployer"
    private final String DEPLOYER_CLIENT_SECRET = "jenkins-keycloak-client"
    private final String KEYCLOAK_CR_API_VERSION = "v1.edp.epam.com"

    public String url

    private String realmName
    private String clientName
    private String clientSecret

    Keycloak(BuildContext context) {
        this.context = context
    }

    void init() {
        url = context.platform.getJsonPathValue("keycloak.${KEYCLOAK_CR_API_VERSION}", "main", ".spec.url")
        realmName = context.platform.getJsonPathValue("keycloakrealm.${KEYCLOAK_CR_API_VERSION}", DEPLOYER_REALM_CR,
                ".spec.realmName")
        clientName = context.platform.getJsonPathValue("keycloakclient.${KEYCLOAK_CR_API_VERSION}", DEPLOYER_CLIENT_CR,
                ".spec.clientId")
        clientSecret = context.platform.getSecretValue(DEPLOYER_CLIENT_SECRET, "clientSecret")
    }

    String getDeployerAccessToken() {
        context.logger.info("Receiving deployer service account access token")
        String tokenEndpoint = "${url}/auth/realms/${realmName}/protocol/openid-connect/token"
        def response = context.script.httpRequest url: tokenEndpoint,
                httpMode: 'POST',
                contentType: 'APPLICATION_FORM',
                requestBody: "grant_type=client_credentials&client_id=${clientName}&client_secret=${clientSecret}",
                consoleLogResponseBody: false,
                validResponseCodes: "200"
        String token = new JsonSlurperClassic()
                .parseText(response.content)
                .access_token

        if (token) {
            return token
        } else {
            context.script.error("Token have not been received")
        }
    }

    @Override
    String toString() {
        return "Keycloak{" +
                "url='" + url + '\'' +
                ", realmName='" + realmName + '\'' +
                ", clientName='" + clientName + '\'' +
                '}'
    }
}