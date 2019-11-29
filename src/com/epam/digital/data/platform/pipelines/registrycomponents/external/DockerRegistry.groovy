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
import com.epam.digital.data.platform.pipelines.helper.DecodeHelper

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
        def secretDataJson = context.platform.getAsJson("secret", NEXUS_CI_USER_SECRET)["data"]
        ciUser = DecodeHelper.decodeBase64(secretDataJson["username"])
        ciUserPassword = DecodeHelper.decodeBase64(secretDataJson["password"])
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
