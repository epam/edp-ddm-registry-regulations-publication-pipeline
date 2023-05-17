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
import groovy.json.JsonSlurperClassic

@Stage(name = "import-mock-integrations", buildTool = ["any"], type = [ProjectType.LIBRARY])
class ImportMockIntegrations {
    BuildContext context

    void run() {
        String dirPath = "mock-integrations"
        String wiremockUrlMappings = "http://wiremock:9021/__admin/mappings"
        try {
            def files = context.script.findFiles(glob: 'mock-integrations/*.json')
            boolean filesExists = files.length > 0
            if (filesExists) {
                context.logger.info("Deleting old mappings")
                context.script.httpRequest(
                    httpMode: 'DELETE',
                    validResponseCodes: '200',
                    url: wiremockUrlMappings,
                    quiet: true
                )
                files.each {
                    String formJsonContent = context.script.readFile(file: "${dirPath}/${it.name}", encoding: "UTF-8")
                    context.logger.info("Importing mapping from file ${it.name}")
                    def response = context.script.httpRequest(
                        contentType: 'APPLICATION_JSON_UTF8',
                        httpMode: 'POST',
                        requestBody: formJsonContent,
                        validResponseCodes: '200,422',
                        url: wiremockUrlMappings + "/import"
                    )
                    if (response .status.equals(422)) {
                       String msgResponse = new JsonSlurperClassic().parseText(response.getContent())
                       context.logger.error("Something with mapping ${it.name}. Response: ${msgResponse}")
                    }
                }
                context.logger.info("Saving mappings...")
                context.script.httpRequest(
                    httpMode: 'POST',
                    url: wiremockUrlMappings + "/save",
                    quiet: true
                )
            }
        } catch(any) {
            context.script.error("Something went wrong while importing mappings!")
        }
    }
}
