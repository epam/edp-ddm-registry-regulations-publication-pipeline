/*
 * Copyright 2023 EPAM Systems.
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
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "import-mock-integrations", buildTool = ["any"], type = [ProjectType.LIBRARY])
class ImportMockIntegrations {
    BuildContext context

    void run() {
        String dirPath = "mock-integration"
        String wiremockUrlMappings = "http://wiremock:9021/__admin/mappings"
        try {
            def files = context.script.findFiles(glob: 'mock-integration/*.json')
            if (files[0]) {
                context.script.httpRequest(
                    httpMode: 'DELETE',
                    validResponseCodes: '200',
                    url: wiremockUrlMappings,
                    quiet: true
                )
                files.each {
                    String formJsonContent = context.script.readFile(file: "${dirPath}/${it.name}", encoding: "UTF-8")
                    context.script.httpRequest(
                        customHeaders: [[name: 'Content-Type', value: "application/json"]],
                        httpMode: 'POST',
                        requestBody: formJsonContent,
                        validResponseCodes: '200',
                        url: wiremockUrlMappings + "/import",
                        quiet: true
                    )
                }
            }
        } catch(any) {
            println("[JENKINS][INFO] Cannot import mocks or directory is absent")
        }
    }
}
