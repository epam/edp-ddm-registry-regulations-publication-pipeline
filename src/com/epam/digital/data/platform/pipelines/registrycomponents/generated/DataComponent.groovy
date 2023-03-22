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

package com.epam.digital.data.platform.pipelines.registrycomponents.generated

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.codebase.Codebase
import com.epam.digital.data.platform.pipelines.tools.TemplateRenderer

class DataComponent {
    private final BuildContext context

    public final String name
    public final String fullName
    public final String version
    public final String pipelineName
    public final String codebaseName
    public final String codebaseBranch
    public final String repositoryPath
    public final String jobProvisioner
    public final String jenkinsAgent
    public final String DEPLOY_TEMPLATES_PATH = "deploy-templates"
    public final String BUILD_TOOL = "docker"

    private String workDir
    private boolean isRepositoryCreated = false

    private final String codebaseYaml
    private final String codebaseBranchYaml

    DataComponent(BuildContext context, String name, String codebaseVersion,
                  String codebaseBranch, String repositoryPath,
                  String jobProvisioner, String jenkinsAgent) {
        this.context = context
        this.name = name
        this.fullName = "registry-${name}"
        this.version = codebaseVersion
        this.codebaseName = "${fullName}"
        this.codebaseBranch = codebaseBranch
        this.repositoryPath = repositoryPath
        this.jobProvisioner = jobProvisioner
        this.jenkinsAgent = jenkinsAgent
        this.pipelineName = "${codebaseName}/${codebaseBranch.toUpperCase()}-Build-${codebaseName}"
        this.codebaseYaml = "${fullName}.yaml"
        this.codebaseBranchYaml = "${fullName}.yaml"
    }

    void setWorkDir(String workDir) {
        this.workDir = workDir
    }

    String getWorkDir() {
        return workDir
    }

    void createCodebaseCR() {
        if (!context.platform.checkObjectExists(Codebase.CODEBASE_CR, codebaseName)) {
            Map binding = [
                    "codebaseName"  : codebaseName,
                    "defaultBranch" : codebaseBranch,
                    "version"       : version,
                    "buildTool"     : BUILD_TOOL,
                    "jenkinsAgent"  : jenkinsAgent,
                    "jobProvisioner": jobProvisioner
            ]
            String template = context.script.libraryResource("${context.YAML_RESOURCES_RELATIVE_PATH}" +
                    "/codebase/data-component.yaml")
            context.script.writeFile(file: codebaseYaml, text: TemplateRenderer.renderTemplate(template, binding))
            context.platform.apply(codebaseYaml)
        }
    }

    void createCodebaseBranchCR() {
        String name = "${name}-release-${version}"
        if (!context.platform.checkObjectExists(Codebase.CODEBASEBRANCH_CR, name)) {
            Map binding = [
                    "codebaseName": codebaseName,
                    "branch"      : codebaseBranch,
                    "version"     : version
            ]
            String template = context.script.libraryResource("${context.YAML_RESOURCES_RELATIVE_PATH}" +
                    "/codebasebranch/data-component.yaml")
            context.script.writeFile(file: codebaseBranchYaml, text: TemplateRenderer.renderTemplate(template, binding))
            context.platform.apply(codebaseBranchYaml)
        }
    }

    boolean getIsRepositoryCreated() {
        return isRepositoryCreated
    }

    void setIsRepositoryCreated(boolean isRepositoryCreated) {
        this.isRepositoryCreated = isRepositoryCreated
    }

    @Override
    String toString() {
        return "DataComponent{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", pipelineName='" + pipelineName + '\'' +
                ", codebaseName='" + codebaseName + '\'' +
                ", codebaseBranch='" + codebaseBranch + '\'' +
                '}'
    }
}
