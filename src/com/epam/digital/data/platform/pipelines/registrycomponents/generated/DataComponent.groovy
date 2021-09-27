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

    DataComponent(BuildContext context, String name, String registryName, String registryVersion,
                  String codebaseBranch, String repositoryPath,
                  String jobProvisioner, String jenkinsAgent) {
        this.context = context
        this.name = name
        this.fullName = "${registryName}-${name}"
        this.version = registryVersion
        this.codebaseName = "${registryName}-${name}-${registryVersion}"
        this.codebaseBranch = codebaseBranch
        this.repositoryPath = repositoryPath
        this.jobProvisioner = jobProvisioner
        this.jenkinsAgent = jenkinsAgent
        this.pipelineName = "${codebaseName}/${codebaseBranch.toUpperCase()}-Build-${codebaseName}"
        this.codebaseYaml = "${name}.yaml"
        this.codebaseBranchYaml = "${name}.yaml"
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
