/*
 * Copyright 2021 EPAM Systems.
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

class Openshift extends Kubernetes {
    Openshift(BuildContext context, String CLI = "oc") {
        super(context, CLI)
    }

    void addSccToUser(String user, String scc, String project) {
        context.script.sh(script: "${CLI} adm policy add-scc-to-user ${scc} -z ${user} -n ${project}")
    }
}
