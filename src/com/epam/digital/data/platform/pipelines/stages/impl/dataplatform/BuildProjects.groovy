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

package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registrycomponents.generated.DataComponent
import com.epam.digital.data.platform.pipelines.registrycomponents.generated.DataComponentType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "build-projects", buildTool = ["any"], type = [ProjectType.LIBRARY])
class BuildProjects {
    BuildContext context

    void run() {
        context.logger.info("Building the data model first")
        DataComponent dataModel = context.dataComponents.get(DataComponentType.MODEL.getValue())
        context.script.build job: dataModel.pipelineName, wait: true, propagate: true

        LinkedHashMap parallelBuilds = [:]
        context.dataComponents.values().each { dataComponent ->
            parallelBuilds["${dataComponent.name}"] = {
                if (dataComponent.name != dataModel.name) {
                    context.logger.info("Building ${dataComponent.name}")
                    context.script.build job: dataComponent.pipelineName, wait: true, propagate: true
                }
            }
        }
        context.script.parallel(parallelBuilds)
    }
}
