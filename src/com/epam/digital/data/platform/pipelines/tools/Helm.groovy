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

package com.epam.digital.data.platform.pipelines.tools

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class Helm {
    static void upgrade(BuildContext context, String releaseName, String deployTemplatesPath,
                        LinkedHashMap<String, String> parameters, String fileParameters = '', String namespace, boolean wait = false) {
        String parametersString = ""
        parameters.each {
            parametersString += "--set ${it.key}=${it.value} "
        }

        if (wait)
            parametersString += "--wait "

        context.script.sh(script: "helm upgrade --install ${releaseName} ${deployTemplatesPath} " +
                "${fileParameters} --namespace ${namespace} ${parametersString}")
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
