package com.epam.digital.data.platform.pipelines.platform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class Openshift extends Kubernetes {
    Openshift(BuildContext context, String CLI = "oc") {
        super(context, CLI)
    }

    void addSccToUser(String user, String scc, String project) {
        context.script.sh(script: "${CLI} adm policy add-scc-to-user ${scc} -z ${user} -n ${project}")
    }
}
