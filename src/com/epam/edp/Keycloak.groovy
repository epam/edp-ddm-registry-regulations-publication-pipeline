/* Copyright 2018 EPAM Systems.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

 See the License for the specific language governing permissions and
 limitations under the License.*/

package com.epam.edp

import com.epam.edp.platform.Platform
import groovy.json.JsonSlurperClassic

class Keycloak {
    Script script
    Platform platform
    Job job

    def jenkinsDeployerToken

    Keycloak(job, platform, script) {
        this.script = script
        this.job = job
        this.platform = platform
    }   

    def init() {
        try {
            this.jenkinsDeployerToken = getAccessToken("jenkins-keycloak-client", "jenkins-deployer", "admin")
        } catch (any) {
            this.jenkinsDeployerToken = getAccessToken("jenkins-keycloak-client", "jenkins-deployer", "main")
        }
    }


    def getJenkinsKeycloakCredentials(secretName) {
        def credentials = [:]
        credentials.clientSecret = new String(platform.getJsonPathValue("secret", secretName,
                ".data.clientSecret").decodeBase64())
        return credentials
    }

    def getAccessToken(secretName, keycloakClientName, keycloakRealmName) {
        def keycloakUrl = "${platform.getJsonPathValue("keycloaks", "main", ".spec.url")}"
        def keycloakRealm = "${platform.getJsonPathValue("keycloakrealm", keycloakRealmName, ".spec.realmName")}"
        def keycloakClient = "${platform.getJsonPathValue("keycloakclient", keycloakClientName, ".spec.clientId")}"
        def tokenEndpoint = "${keycloakUrl}/auth/realms/${keycloakRealm}/protocol/openid-connect/token"

        def credentials = getJenkinsKeycloakCredentials(secretName)
        def response = script.httpRequest url: tokenEndpoint,
            httpMode: 'POST',
            contentType: 'APPLICATION_FORM',
            requestBody: "grant_type=client_credentials&client_id=${keycloakClient}&client_secret=${credentials.clientSecret}",
            consoleLogResponseBody: false

        return new JsonSlurperClassic()
            .parseText(response.content)
            .access_token
    }
}