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

package com.epam.digital.data.platform.pipelines.platform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import groovy.json.JsonSlurperClassic

class Kubernetes implements IPlatform {
    protected BuildContext context

    protected final String CLI

    Kubernetes(BuildContext context, String CLI = "kubectl") {
        this.context = context
        this.CLI = CLI
    }

    @Override
    void addSccToUser(String user, String scc, String project) {
        context.logger.warn("Security model for kubernetes hasn't defined yet")
    }

    @Override
    void waitFor(String resource, String condition, String timeout) {
        context.script.sh(script: "${CLI} wait --for=${condition} ${resource} --timeout=${timeout}")
    }

    @Override
    String getSecretValue(String secretName, String secretKey) {
        return new String(getJsonPathValue("secret", secretName, ".data.${secretKey}").decodeBase64())
    }

    @Override
    String getJsonPathValue(String resourceApi, String resourceName, String jsonPath, String project = null) {
        String command = "${CLI} get ${resourceApi} ${resourceName} -o jsonpath='{${jsonPath}}'"
        if (project)
            command = "${command} -n ${project}"
        return context.script.sh(script: command, returnStdout: true)
    }

    @Override
    String podExec(String podName, String command, String container = "") {
        if (container)
            podName += " -c ${container}"
        context.script.sh(script: "${CLI} exec ${podName} -- ${command}", returnStdout: true)
    }

    @Override
    void triggerDeploymentRollout(String deploymentName) {
        /* "oc rollout" does not support kind Deployment and "kubectl rollout restart" is available only since
        * Kubernetes 1.15 so we just patch date annotation to trigger new rollout */
        LinkedHashMap parallelRollout = [:]
        deploymentName.split(',').each {
            parallelRollout["${it}"] = {
                patch("deployment", it, "\'{\"spec\":{\"template\":" +
                        "{\"metadata\":{\"annotations\":{\"reload_by\":\"${context.script.env.BUILD_TAG}\"}}}}}\'")
                patch("deployment", it, "\'{\"spec\":{\"template\":" +
                        "{\"metadata\":{\"annotations\":{\"date\":\"${new Date()}\"}}}}}\'")
                context.script.sleep(5)
                context.script.sh(script: "${CLI} rollout status deployment ${it}")
            }
        }
        context.script.parallel(parallelRollout)
    }

    @Override
    boolean patch(String resource, String name, String jsonpath) {
        String result = context.script.sh(script: "${CLI} patch ${resource} ${name} --type merge -p ${jsonpath}",
                returnStdout: true)
        return !result.contains("not patched")
    }

    @Override
    void create(String resource, String name, String parameters = "") {
        context.script.sh(script: "${CLI} create ${resource} ${name} ${parameters}")
    }

    @Override
    boolean patchConfigMapKey(String configMap, String key, String value) {
        String result = context.script.sh(script: "${CLI} patch cm ${configMap} --type merge " +
                "-p \"{\\\"data\\\": {\\\"${key}\\\":\\\"${value}\\\"}}\"", returnStdout: true)
        return !result.contains("not patched")
    }

    @Override
    void apply(String filePath) {
        context.script.sh(script: "${CLI} create -f ${filePath} || ${CLI} replace -f ${filePath}")
    }

    @Override
    void deleteObject(String objectType, String objectName, String parameters = "", Boolean force = false) {
        String command = "${CLI} delete ${objectType} ${objectName} ${parameters}"
        if (force)
            command = "${command} --force --grace-period=0"
        context.script.sh(script: "${command} || true")
    }

    @Override
    boolean checkObjectExists(String objectType, String objectName, String project = null) {
        String command = "${CLI} get ${objectType} ${objectName} --ignore-not-found=true"
        if (project)
            command = "${command} -n ${project}"
        String commandOutput = context.script.sh(script: command, returnStdout: true).trim()
        context.logger.info("Get object command output: " + commandOutput)
        if (commandOutput == "")
            return false
        return true
    }

    @Override
    void annotate(String resource, String name, String key, String value, boolean overwrite = false) {
        context.script.sh(script: "${CLI} annotate ${resource} ${name} ${key}=${value} --overwrite=${overwrite}")
    }

    @Override
    void scale(String resource, int replicas) {
        context.script.sh(script: "${CLI} scale ${resource} --replicas=${replicas}")
    }

    @Override
    String get(String resource, String name, String parameters = "") {
        context.script.sh(script: "${CLI} get ${resource} ${name} ${parameters}", returnStdout: true)
    }

    @Override
    LinkedHashMap getAsJson(String resource, String name) {
        String text = context.script.sh(script: "${CLI} get ${resource} ${name} -ojson", returnStdout: true)
        def parsedJson = new JsonSlurperClassic().parseText(text)
        return parsedJson as LinkedHashMap
    }

    @Override
    String getAll(String resource, String parameters = "") {
        context.script.sh(script: "${CLI} get ${resource} ${parameters}", returnStdout: true)
    }
}
