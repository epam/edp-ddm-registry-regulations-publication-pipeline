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

package com.epam.digital.data.platform.pipelines.codebase

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.buildtool.BuildToolFactory
import com.epam.digital.data.platform.pipelines.buildtool.IBuildTool
import com.epam.digital.data.platform.pipelines.stages.ProjectType

class Codebase {
    public final static String CODEBASE_API_GROUP = "v2.edp.epam.com"
    public final static String CODEBASE_CR = "codebase.${CODEBASE_API_GROUP}"
    public final static String CODEBASEBRANCH_CR = "codebasebranch.${CODEBASE_API_GROUP}"

    private final BuildContext context

    private String imageName
    private String imageTag

    public String name
    public String branch
    public String defaultBranch
    public String imageUrl
    public String buildConfigName
    public String repositoryPath
    public String jobProvisioner
    public String type
    public String jenkinsAgent
    public String buildToolSpec
    public IBuildTool buildTool
    public String sourceRepository

    Codebase(BuildContext context) {
        this.context = context
    }

    String getImageTag() {
        return imageTag
    }

    void setImageTag(String imageTag) {
        this.imageTag = imageTag
        this.imageUrl = "${context.dockerRegistry.host}/${context.namespace}/${imageName}:${imageTag}"
    }

    void init() {
        this.name = context.getParameterValue("CODEBASE_NAME", "registry")
        this.defaultBranch = getCodebaseSpecField("defaultBranch")
        this.branch = context.getParameterValue("CODEBASE_BRANCH", this.defaultBranch).toLowerCase()
        this.repositoryPath = context.getParameterValue("REPOSITORY_PATH")
        this.jobProvisioner = getCodebaseSpecField("jobProvisioning")
        this.sourceRepository = getCodebaseSpecField("repository.url")
        this.type = getCodebaseSpecField("type").toLowerCase()
        if (type == ProjectType.APPLICATION.getValue()) {
            this.imageName = "${name}-${branch}"
            this.imageTag = "latest"
            this.imageUrl = "${context.dockerRegistry.host}/${context.namespace}/${imageName}:${imageTag}"
            this.buildConfigName = imageName.replaceAll('\\.', "-")
        }
        this.jenkinsAgent = getCodebaseSpecField("jenkinsSlave")
        this.buildToolSpec = getCodebaseSpecField("buildTool")
        this.buildTool = BuildToolFactory.getBuildToolImpl(buildToolSpec, context)
    }

    void initBuildTool() {
        buildTool.init()
    }

    private String getCodebaseSpecField(final String field) {
        return context.platform.getJsonPathValue(CODEBASE_CR, name, ".spec.${field}")
    }

    @Override
    String toString() {
        return "Codebase{" +
                "name='" + name + '\'' +
                ", branch='" + branch + '\'' +
                ", defaultBranch='" + defaultBranch + '\'' +
                ", imageName='" + imageName + '\'' +
                ", imageTag='" + imageTag + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", buildConfigName='" + buildConfigName + '\'' +
                ", repositoryPath='" + repositoryPath + '\'' +
                ", jobProvisioner='" + jobProvisioner + '\'' +
                ", type='" + type + '\'' +
                ", jenkinsAgent='" + jenkinsAgent + '\'' +
                '}'
    }
}
