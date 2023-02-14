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

package com.epam.digital.data.platform.pipelines.registry

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.codebase.Codebase
import groovy.json.JsonSlurperClassic

class CleanupRegistryRegulation {

    private final BuildContext context

    CleanupRegistryRegulation(BuildContext context) {
        this.context = context
    }

    void createResource(String resourceName) {
        String tmpFile = "tmp-${resourceName}.yaml"
        try {
            context.script.sh("rm -f ${tmpFile}")
        } catch (any) {
            context.logger.info("File ${tmpFile} does not exist")
        }
        LinkedHashMap resourceConfig = context.script.readYaml(text: context.platform.get("configmap",
                resourceName, "-o yaml"))
        context.script.writeYaml(file: tmpFile, data: resourceConfig["data"][resourceName])
        context.script.sh("sed -i 1d ${tmpFile}; sed -i 's/  ---/---/' ${tmpFile}")
        try {
            context.platform.apply(tmpFile)
        } catch (any) {
            context.logger.info("Failed to create $resourceName")
        }
        context.script.sh("rm -f ${tmpFile}")
    }

    void createTempSecret(String codebaseName, String sourceSecretName = "edp-gerrit-ciuser") {
        String getUsername = context.platform.getSecretValue(sourceSecretName, "username")
        String getPassword = context.platform.getSecretValue(sourceSecretName, "password")
        String tmpGerritSecret = "repository-codebase-${codebaseName}-temp"
        if (!context.platform.checkObjectExists("secret", tmpGerritSecret)) {
            context.platform.create("secret generic", tmpGerritSecret,
                    "--from-literal='username=$getUsername' --from-literal='password=$getPassword'")
        }
    }

    void recreateDefaultCodebaseRelatedResources(String resourceName) {
        checkFinalizers(resourceName)
        checkRepository(resourceName)
        boolean checkCodebase = context.platform.checkObjectExists("codebase", resourceName)
        String checkCodebaseBranch = context.platform.get("codebasebranch", "",
                "-l affiliatedWith=$resourceName")
        if (!checkCodebase || !checkCodebaseBranch) {
            context.gitServer.deleteRepository(resourceName)
            createTempSecret(resourceName)
            createResource(resourceName)
        }
    }

    void checkRepository(String repositoryName) {
        boolean isResourceNameRepositoryExists = context.gitServer.isRepositoryExists(repositoryName)
        if (!isResourceNameRepositoryExists) {
            removeFinalizers(repositoryName)
            context.platform.deleteObject(Codebase.CODEBASEBRANCH_CR, "-l affiliatedWith=$repositoryName")
            context.platform.deleteObject(Codebase.CODEBASE_CR, repositoryName)
        }
    }

    void checkFinalizers(String codebaseName) {
        String checkCodebaseFinalizers = ''
        String checkCodebaseBranchFinalizers = ''
        try {
            checkCodebaseFinalizers = context.platform.get("codebase", codebaseName, "-o json")
            checkCodebaseBranchFinalizers = context.platform.get("codebasebranch", "",
                    "-l affiliatedWith=$codebaseName -o json")
        } catch (any) {
            context.logger.info("Failed to retrieve $codebaseName json")
        }
        if (checkCodebaseFinalizers.contains("deletionTimestamp") || checkCodebaseBranchFinalizers.contains("deletionTimestamp")) {
            removeFinalizers(codebaseName)
        }
    }

    void removeFinalizers(String codebaseName) {
        try {
            context.platform.patch(Codebase.CODEBASE_CR, codebaseName, "\'{\"metadata\":{\"finalizers\":[]}}\'")
            context.platform.patchByLabel(Codebase.CODEBASEBRANCH_CR, "affiliatedWith=$codebaseName",
                    "\'{\"metadata\":{\"finalizers\":[]}}\'")
            context.gitServer.deleteRepository(codebaseName)
        } catch (any) {
            context.logger.info("Failed to patch $codebaseName resources")
        }
    }

    void trigerManualNexusTask() {
        String taskType = "repository.cleanup"
        String nexusUrl = "http://nexus:8081/nexus/service/rest/v1/tasks"
        String nexusCredentialsId = context.dockerRegistry.NEXUS_CI_USER_SECRET
        def taskId = ""
        def nexusGetResponse = context.script.httpRequest url: nexusUrl,
                httpMode: 'GET',
                authentication: nexusCredentialsId,
                validResponseCodes: '200,404',
                quiet: true
        if (nexusGetResponse.status.equals(200))
            taskId = new JsonSlurperClassic().parseText(nexusGetResponse.content).items.findAll { it.type.equals(taskType) }.id[0]
        context.script.httpRequest url: nexusUrl + "/" + taskId + "/run",
                httpMode: 'POST',
                authentication: nexusCredentialsId,
                customHeaders: [[name: 'Content-Type', value: "application/json"]],
                validResponseCodes: '204',
                quiet: true
    }
}
