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

package com.epam.digital.data.platform.pipelines.registrycomponents.external

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import groovy.json.JsonSlurperClassic

class Keycloak {
    private final BuildContext context

    public final static String KEYCLOAK_API = "keycloak.${KEYCLOAK_CR_API_VERSION}"
    public final static String KEYCLOAK_REALM_API = "keycloakrealm.${KEYCLOAK_CR_API_VERSION}"
    public final static String KEYCLOAK_CLIENT_API = "keycloakclient.${KEYCLOAK_CR_API_VERSION}"
    public final static String KEYCLOAK_CR_API_VERSION = "v1.edp.epam.com"

    public String url

    Keycloak(BuildContext context) {
        this.context = context
    }

    void init() {
        url = context.platform.getJsonPathValue(KEYCLOAK_API, "main", ".spec.url")
    }

    String getAccessToken(KeycloakClient kc) {
        context.logger.info("Receiving ${kc.clientId} access token")
        String token
        String tokenEndpoint = "${url}/auth/realms/${kc.realm}/protocol/openid-connect/token"
        int maxAttempts = 5
        int attempt = 0
        boolean requestStatus = false
        while (!requestStatus) {
            attempt++
            if (attempt == maxAttempts) break
            try {
                def response = context.script.httpRequest url: tokenEndpoint,
                        httpMode: 'POST',
                        contentType: 'APPLICATION_FORM',
                        requestBody: "grant_type=client_credentials&client_id=${kc.clientId}&client_secret=${kc.clientSecret}",
                        consoleLogResponseBody: false,
                        validResponseCodes: "200"
                requestStatus = true
                token = new JsonSlurperClassic()
                        .parseText(response.content)
                        .access_token
            }
            catch (any) {
                requestStatus = false
                context.script.sleep(5)
            }
        }

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
                '}'
    }
}