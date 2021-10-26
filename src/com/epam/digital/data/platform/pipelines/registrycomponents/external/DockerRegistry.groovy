package com.epam.digital.data.platform.pipelines.registrycomponents.external

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class DockerRegistry {
    private final BuildContext context

    public final String NEXUS_CI_USER_SECRET = "nexus-ci.user"
    public final String PUSH_SECRET = "nexus-docker-registry-namespaced"

    public String ciUser
    public String ciUserPassword
    public String host
    public String proxyHost

    DockerRegistry(BuildContext context) {
        this.context = context
    }

    void init() {
        ciUser = context.platform.getSecretValue(NEXUS_CI_USER_SECRET, "username")
        ciUserPassword = context.platform.getSecretValue(NEXUS_CI_USER_SECRET, "password")
        host = context.platform.getJsonPathValue("edpcomponent", "docker-registry", ".spec.url")
        proxyHost = context.platform.getJsonPathValue("edpcomponent", "docker-proxy-registry", ".spec.url")
        if (!context.platform.checkObjectExists("secret", PUSH_SECRET)) {
            try {
                context.platform.create("secret docker-registry", PUSH_SECRET, "" +
                        "--docker-username=${ciUser} " +
                        "--docker-password=${ciUserPassword} " +
                        "--docker-server=${host} " +
                        "--docker-email=ci.user@mdtu-ddm.edp.com")
            }
            catch (any) {
                if (context.platform.checkObjectExists("secret", PUSH_SECRET)) {
                    context.logger.info("Secret $PUSH_SECRET has already been created")
                } else {
                    context.script.error("Secret $PUSH_SECRET does not exist")
                }
            }
        }
    }

    @Override
    String toString() {
        return "DockerRegistry{" +
                "host='" + host + '\'' +
                ", proxyHost='" + proxyHost + '\'' +
                '}'
    }
}
