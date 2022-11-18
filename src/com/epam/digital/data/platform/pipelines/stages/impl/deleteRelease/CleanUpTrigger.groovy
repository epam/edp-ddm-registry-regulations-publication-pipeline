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

package com.epam.digital.data.platform.pipelines.stages.impl.deleteRelease

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.codebase.Codebase
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "cleanup-trigger", buildTool = ["any"], type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class CleanUpTrigger {
    BuildContext context

    void run() {
        context.script.timeout(unit: 'MINUTES', time: 30) {
            LinkedHashMap parallelDeletion = [:]

            context.logger.info("Get current codebase and codebasebranch CRs")
            getCurrentCodebaseCRs("codebase", "recreateByCleanup=true")
            getCurrentCodebaseCRs("codebasebranch", "recreateByCleanup=true")
            getCurrentCodebaseCRs("job", "job-name=create-dashboard-job")
            parallelDeletion["clearDataFromRegulationManagement"] = {
                ArrayList registryRegulationManagementPods = context.script.sh(script: "kubectl get pod -l app=registry-regulation-management " +
                        "-o jsonpath='{range .items[*]}{.metadata.name}{\"\\n\"}{end}' -n ${context.namespace}", returnStdout: true).tokenize('\n')
                String registryRegulationManagementPodRepositoriesData = context.script.sh(script: "kubectl get pod -l app=registry-regulation-management " +
                        "-o jsonpath='{.items[*].spec.containers[*].volumeMounts[?(@.name==\"repositories-data\")].mountPath}' -n ${context.namespace}", returnStdout: true).trim()

                registryRegulationManagementPods.each { pod ->
                    context.script.sh(script: "kubectl exec -n ${context.namespace} ${pod} -c registry-regulation-management -- bash -c \'rm -rf ${registryRegulationManagementPodRepositoriesData}/*\'")
                }
            }
            parallelDeletion["removeDataServices"] = {
                context.logger.info("Removing Data Services codebasebranches")
                context.platform.deleteObject(Codebase.CODEBASEBRANCH_CR, "-l type=data-component")
                context.platform.deleteObject(Codebase.CODEBASE_CR, "-l type=data-component")
            }
            parallelDeletion["removeRegistryRegulation"] = {
                context.logger.info("Removing registry-regulations codebasebranch and codebase CRs")
                context.platform.deleteObject(Codebase.CODEBASEBRANCH_CR, "-l affiliatedWith=$context.codebase.name")
                context.platform.deleteObject(Codebase.CODEBASE_CR, context.codebase.name)
                context.logger.info("Removing ${context.codebase.name} repo")
                context.gitServer.deleteRepository(context.codebase.name)
            }

            parallelDeletion["removeHistoryExcerptor"] = {
                context.logger.info("Removing history-excerptor codebasebranch and codebase CRs")
                context.platform.deleteObject(Codebase.CODEBASEBRANCH_CR, "-l affiliatedWith=$context.codebase.historyName")
                context.platform.deleteObject(Codebase.CODEBASE_CR, context.codebase.historyName)
            }
            context.script.parallel(parallelDeletion)

            [context.codebase.name, context.codebase.historyName].each {
                String tmpGerritSecret = "repository-codebase-${it}-temp"
                if (!context.platform.checkObjectExists("secret", tmpGerritSecret)) {
                    String edpGerritSecret = "edp-gerrit-ciuser"
                    context.platform.create("secret generic", tmpGerritSecret,
                            "--from-literal='username=${context.platform.getSecretValue(edpGerritSecret, "username")}' " +
                                    "--from-literal='password=${context.platform.getSecretValue(edpGerritSecret, "password")}'")
                }
            }

            context.logger.info("Creating ${context.codebase.name} codebase, codebasebranch and redash job")
            ["codebase", "codebasebranch", "job"].each {
                context.platform.apply("resources-${it}.json")
            }
        }
    }

    void getCurrentCodebaseCRs(String resourceType, String label) {
        String fileName = "resources-${resourceType}.json"
        String currentCr
        if (resourceType == 'job') {
            currentCr = context.script.sh(script: "oc get $resourceType -l $label -o json | " +
                    "jq 'del(.items[].metadata.resourceVersion,.items[].metadata.uid,.items[].metadata.managedFields," +
                    ".items[].metadata.creationTimestamp,.items[].metadata.generation,.items[].metadata.finalizers," +
                    ".items[].metadata.labels.\"controller-uid\",.items[].spec.selector,.items[].spec.template.metadata.labels.\"controller-uid\"," +
                    ".items[].status)'", returnStdout: true)
        } else {
            currentCr = context.script.sh(script: "oc get $resourceType -l $label -o json | " +
                    "jq 'del(.items[].metadata.resourceVersion,.items[].metadata.uid,.items[].metadata.managedFields," +
                    ".items[].metadata.selfLink,.items[].metadata.ownerReferences,.items[].metadata.creationTimestamp," +
                    ".items[].metadata.generation,.items[].metadata.finalizers,.items[].status)'", returnStdout: true)
        }
        context.script.writeFile(file: fileName, text: currentCr)

    }
}
