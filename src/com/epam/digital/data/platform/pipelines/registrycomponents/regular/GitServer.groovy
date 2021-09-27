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

    GitServer(BuildContext context, String name) {
        this.name = name
        this.context = context
    }

    void init() {
        this.credentialsId = getGitserverSpecField("nameSshKeySecret")
        this.autouser = getGitserverSpecField("gitUser")
        this.host = getGitserverSpecField("gitHost")
        this.sshPort = getGitserverSpecField("sshPort")
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
