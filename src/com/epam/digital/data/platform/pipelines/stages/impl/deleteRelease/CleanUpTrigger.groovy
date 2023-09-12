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
    String redashJobName = "create-dashboard-job"
    void run() {

        LinkedHashMap parallelDeletion = [:]
        boolean deleteRegistryRegulationsGerritRepo = context.getParameterValue("DELETE_REGISTRY_REGULATIONS_GERRIT_REPOSITORY", "false").toBoolean()

        context.logger.info("Calculate the timeout for cleanup job")
        int registryRegulationsVersions = context.script.sh(script: "oc -n ${context.namespace} get codebases " +
                "--no-headers | wc -l ", returnStdout: true).trim().toInteger()
        int cleanUpTimeout = (registryRegulationsVersions == 0) ? 5 : registryRegulationsVersions * 3
        context.logger.info("Timeout is $cleanUpTimeout minutes")

        parallelDeletion["clearDataFromRegulationManagement"] = {
            ArrayList registryRegulationManagementPods = context.script.sh(script: "kubectl get pod -l app=registry-regulation-management " +
                    "-o jsonpath='{range .items[*]}{.metadata.name}{\"\\n\"}{end}' -n ${context.namespace}", returnStdout: true).tokenize('\n')
            String registryRegulationManagementPodRepositoriesData = context.script.sh(script: "kubectl get pod -l app=registry-regulation-management " +
                    "-o jsonpath='{.items[*].spec.containers[*].volumeMounts[?(@.name==\"repositories-data\")].mountPath}' -n ${context.namespace}", returnStdout: true).trim()

            registryRegulationManagementPods.each { pod ->
                context.script.sh(script: "kubectl exec -n ${context.namespace} ${pod} -c registry-regulation-management -- bash -c \'rm -rf ${registryRegulationManagementPodRepositoriesData}/*\'")
            }
        }

        // Remove multiple-version of data-services
        parallelDeletion["removeDataServices"] = {
            context.logger.info("Removing Data Services codebasebranches")
            try {
                context.script.timeout(unit: 'MINUTES', time: cleanUpTimeout) {
                    context.platform.deleteObject(Codebase.CODEBASEBRANCH_CR, "-l type=data-component")
                    context.platform.deleteObject(Codebase.CODEBASE_CR, "-l type=data-component")
                }
            } catch (any) {
                context.script.error("Cannot gracefully remove data services codebase and codebasebranch CRs")
            }
        }

        parallelDeletion["removeHistoryExcerptor"] = {
            context.logger.info("Removing ${context.codebase.historyName} codebasebranch and codebase CRs")
            try {
                context.script.timeout(unit: 'MINUTES', time: cleanUpTimeout) {
                    context.platform.deleteObject(Codebase.CODEBASEBRANCH_CR, "-l affiliatedWith=$context.codebase.historyName")
                    context.platform.deleteObject(Codebase.CODEBASE_CR, context.codebase.historyName)
                }
            } catch (any) {
                context.script.error("Cannot gracefully remove ${context.codebase.historyName} codebase and codebasebranch CRs")
            }
        }

        parallelDeletion["cleanUpNexus"] = {
            context.logger.info("Remove artifacts from Nexus repositories")
            ["Cleanup service":true, "docker-delete-unused-manifest-and-tags":true,
             "rebuild-repository-index":true, "compact-blobstore-docker-registry":true,
             "compact-blobstore-edp-maven":true].each { taskName, waitTask ->
                context.cleanup.triggerManualNexusTask(taskName, waitTask)
            }
        }
        context.script.parallel(parallelDeletion)

        if (deleteRegistryRegulationsGerritRepo) {
            try {
                context.script.timeout(unit: 'MINUTES', time: 10) {
                    context.logger.info("Removing ${context.codebase.name} codebasebranch and codebase CRs")
                    context.platform.deleteObject(Codebase.CODEBASEBRANCH_CR, "-l affiliatedWith=$context.codebase.name")
                    context.platform.deleteObject(Codebase.CODEBASE_CR, context.codebase.name)
                    context.platform.deleteObject("secret", "registry-regulation-state")
                }
            } catch (any) {
                context.script.error("Cannot gracefully remove ${context.codebase.name} codebase, codebasebranch CRs or registry-regulation-state secret")
            }
            context.logger.info("Removing ${context.codebase.name} repo")
            context.gitServer.deleteRepository(context.codebase.name)

            context.logger.info("Create ${context.codebase.name} and ${redashJobName} resources")
            context.cleanup.createTempSecret(context.codebase.name)
            context.cleanup.createResource(context.codebase.name)
            context.cleanup.createResource(redashJobName)
        } else {
            try {
                context.logger.info("Execute Delete-release-${context.codebase.name} pipeline")
                context.script.build job: "${context.codebase.name}/Delete-release-${context.codebase.name}",
                        wait: true, propagate: true
            } catch (any) {
                context.script.error("Delete-release-${context.codebase.name} pipeline has been failed")
            }
            try {
                context.logger.info("Recreate ${redashJobName} job")
                context.cleanup.createResource(redashJobName)
                context.script.retry(3) {
                    context.platform.waitFor("job/${redashJobName}", "condition=complete", "60s")
                }
                context.logger.info("Execute Build-${context.codebase.name} pipeline")
                context.script.build job: "${context.codebase.name}/MASTER-Build-${context.codebase.name}",
                        wait: false, propagate: true, parameters: [[$class: 'BooleanParameterValue',
                                                                    name  : 'FULL_DEPLOY', value: true]]
            } catch (any) {
                context.script.error("MASTER-Build-${context.codebase.name} pipeline has been failed")
            }
        }

        context.logger.info("Creating ${context.codebase.historyName} codebase resources")
        context.cleanup.createTempSecret(context.codebase.historyName)
        context.cleanup.createResource(context.codebase.historyName)

    }
}
