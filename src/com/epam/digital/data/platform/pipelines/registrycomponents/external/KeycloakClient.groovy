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

    void init (String realm, String clientId, String clientSecret) {
        this.realm = context.platform.getJsonPathValue(Keycloak.KEYCLOAK_REALM_API, realm,
                ".spec.realmName")
        this.clientId = context.platform.getJsonPathValue(Keycloak.KEYCLOAK_CLIENT_API, clientId,
                ".spec.clientId")
        this.clientSecret = context.platform.getSecretValue(clientSecret, "clientSecret")
    }
}
