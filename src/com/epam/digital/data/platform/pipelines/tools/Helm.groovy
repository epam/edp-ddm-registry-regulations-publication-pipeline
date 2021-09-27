package com.epam.digital.data.platform.pipelines.tools

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class Helm {
    static void upgrade(BuildContext context, String releaseName, String deployTemplatesPath,
                        LinkedHashMap<String, String> parameters, String namespace) {
        String parametersString = ""
        parameters.each {
            parametersString += "--set ${it.key}=${it.value} "
        }

        context.script.sh(script: "helm upgrade --install ${releaseName} ${deployTemplatesPath} " +
                "--namespace ${namespace} ${parametersString}")
    }

    static void uninstall(BuildContext context, String releaseName, String namespace,
                          boolean ignoreNotFound = false) {
        context.script.sh(script: "helm delete ${releaseName} -n ${namespace} || ${ignoreNotFound}")
    }
}
