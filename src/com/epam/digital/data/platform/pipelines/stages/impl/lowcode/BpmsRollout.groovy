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

package com.epam.digital.data.platform.pipelines.stages.impl.lowcode

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.BusinessProcMgmtSys
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "bpms-rollout", buildTool = ["any"], type = ProjectType.LIBRARY)
class BpmsRollout {
    BuildContext context

    void run() {
        List array = new ArrayList(context.bpmsConfigMapsChanged.values())
        for (def entry : array) {
            if (entry) {
                context.platform.triggerDeploymentRollout(BusinessProcMgmtSys.BPMS_DEPLOYMENT_NAME)
                return
            }
        }
    }
}
