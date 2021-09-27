package com.epam.digital.data.platform.pipelines.tools

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class Helm {
    static void upgrade(BuildContext context, String releaseName, String deployTemplatesPath,
                        LinkedHashMap<String, String> parameters, String namespace, boolean wait = false) {
        String parametersString = ""
        parameters.each {
            parametersString += "--set ${it.key}=${it.value} "
        }

        if (wait)
            parametersString += "--wait "

        context.script.sh(script: "helm upgrade --install ${releaseName} ${deployTemplatesPath} " +
                "--namespace ${namespace} ${parametersString}")
    }

    static void uninstall(BuildContext context, String releaseName, String namespace,
                          boolean ignoreNotFound = false) {
        context.script.sh(script: "helm delete ${releaseName} -n ${namespace} || ${ignoreNotFound}")
    }

    static String template(BuildContext context, String releaseName, String deployTemplatesPath,
                        LinkedHashMap<String, String> parameters, String namespace, String template = "") {
        String parametersString = ""
        parameters.each {
            parametersString += "--set ${it.key}=${it.value} "
        }

        if (template)
            parametersString += "-s ${template} "

        context.script.sh(script: "helm template ${releaseName} ${deployTemplatesPath} " +
                "--namespace ${namespace} ${parametersString}", returnStdout: true)
    }

}
