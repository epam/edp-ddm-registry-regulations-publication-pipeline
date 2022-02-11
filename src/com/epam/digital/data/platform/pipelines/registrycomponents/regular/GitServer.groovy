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

package com.epam.digital.data.platform.pipelines.registrycomponents.regular

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

abstract class GitServer {
    protected final String GIT_SERVER_CR_API_GROUP = "gitserver.v2.edp.epam.com"

    protected BuildContext context

    public String name
    public String credentialsId
    public String autouser
    public String host
    public String sshPort

    LinkedHashMap gitserverJson

    GitServer(BuildContext context, String name) {
        this.name = name
        this.context = context
    }

    void init() {
        this.gitserverJson = context.platform.getAsJson("gitserver.v2.edp.epam.com", name)["spec"]
        this.credentialsId = this.gitserverJson["nameSshKeySecret"]
        this.autouser = this.gitserverJson["gitUser"]
        this.host = this.gitserverJson["gitHost"]
        this.sshPort = this.gitserverJson["sshPort"]
    }

    protected void getGitserverSpecField(final String field) {
        context.platform.getJsonPathValue(GIT_SERVER_CR_API_GROUP, name, ".spec.${field}")
    }

    abstract boolean isRepositoryExists(String repoName)

    abstract boolean deleteRepository(String repoName)


    @Override
    String toString() {
        return "GitServer{" +
                "name='" + name + '\'' +
                ", credentialsId='" + credentialsId + '\'' +
                ", autouser='" + autouser + '\'' +
                ", host='" + host + '\'' +
                ", sshPort='" + sshPort + '\'' +
                '}'
    }
}
