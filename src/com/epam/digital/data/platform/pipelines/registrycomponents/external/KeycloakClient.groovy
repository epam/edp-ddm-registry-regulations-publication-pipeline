/*
 * Copyright 2021 EPAM Systems.
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

class KeycloakClient {
    private final BuildContext context

    public String realm
    public String clientId
    public String clientSecret

    KeycloakClient(BuildContext context) {
        this.context = context
    }

    void init(String realm, String clientId, String clientSecret) {
        LinkedHashMap parallelGetValue = [:]
        parallelGetValue["realm"] = {
            this.realm = context.platform.getJsonPathValue(Keycloak.KEYCLOAK_REALM_API, realm,
                    ".spec.realmName")
        }
        parallelGetValue["clientId"] = {
            this.clientId = context.platform.getJsonPathValue(Keycloak.KEYCLOAK_CLIENT_API, clientId,
                    ".spec.clientId")
        }
        parallelGetValue["clientSecret"] = {
            this.clientSecret = context.platform.getSecretValue(clientSecret, "clientSecret")
        }

        context.script.parallel(parallelGetValue)
    }
}
