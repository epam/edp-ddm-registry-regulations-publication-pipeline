package com.epam.digital.data.platform.pipelines.platform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

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

        deploymentName.split(',').each {
            context.script.sh(script: "${CLI} patch deployment ${it} -p \"{\\\"spec\\\":{\\\"template\\\":" +
                    "{\\\"metadata\\\":{\\\"annotations\\\":{\\\"date\\\":\\\"`date +'%s'`\\\"}}}}}\"")
            context.script.sleep(5)
            context.script.sh(script: "${CLI} rollout status deployment ${it}")
        }
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
        context.script.sh(script: "${CLI} apply -f ${filePath}")
    }

    @Override
    void deleteObject(String objectType, String objectName, Boolean force = false) {
        String command = "${CLI} delete ${objectType} ${objectName}"
        if (force)
            command = "${command} --force --grace-period=0"
        context.script.sh(script: "${command} || true")
    }

    @Override
    boolean checkObjectExists(String objectType, String objectName, String project = null) {
        String command = "${CLI} get ${objectType} ${objectName} --ignore-not-found=true"
        if (project)
            command = "${command} -n ${project}"
        if (context.script.sh(script: command, returnStdout: true).trim() == "")
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
    String getAll(String resource, String parameters = "") {
        context.script.sh(script: "${CLI} get ${resource} ${parameters}", returnStdout: true)
    }
}
