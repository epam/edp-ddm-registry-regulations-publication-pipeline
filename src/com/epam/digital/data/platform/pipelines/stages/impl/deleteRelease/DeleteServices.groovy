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
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage
import com.epam.digital.data.platform.pipelines.tools.Helm

@Stage(name = "delete-services", buildTool = ["any"], type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class DeleteServices {
    BuildContext context

    void run() {
        context.logger.info("Removing ${context.codebase.name} helm release")
        def splitCodebaseName = context.codebase.name.split('-')
        if (splitCodebaseName.size() > 2) {
            Helm.uninstall(context, "${splitCodebaseName[0]}-${splitCodebaseName[1]}-${splitCodebaseName[2]}",
                    context.namespace, true)
        }
        context.logger.info("Removing ${context.codebase.name} repo")
        context.gitServer.deleteRepository(context.codebase.name)

        context.logger.info("Removing data components buildconfigs")
        if (context.platform.checkObjectExists("buildconfig", context.codebase.buildConfigName)) {
            context.platform.deleteObject("buildconfig", context.codebase.buildConfigName)
        } else {
            context.logger.info("There is no buildconfig " + context.codebase.buildConfigName)
        }
    }
}
