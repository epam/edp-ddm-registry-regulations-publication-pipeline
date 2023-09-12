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

package com.epam.digital.data.platform.pipelines.stages.impl.lowcode

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.FormShemaProvider
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage
import groovy.json.JsonSlurper

@Stage(name = "upload-form-changes", buildTool = ["any"], type = ProjectType.LIBRARY)
class UploadFormChanges {
    BuildContext context

    void run() {
        ArrayList<String> changedFormFiles
        if (context.getParameterValue("FULL_DEPLOY", "false").toBoolean()) {
            changedFormFiles = context.registryRegulations.getAllRegulations(RegulationType.UI_FORM).join(",").tokenize(',')
        } else {
            changedFormFiles = context.registryRegulations.getChangedStatusOrFiles("plan", "upload-form-changes",
                    "--file-detailed ${context.getWorkDir()}/${RegulationType.UI_FORM.value}")
        }
        if (changedFormFiles) {
            String token = context.keycloak.getAccessToken(context.jenkinsDeployer)
            changedFormFiles.each {
                if (!it.contains(".gitkeep")) {
                    deployForm(it, token)
                }
            }
            context.registryRegulations.getChangedStatusOrFiles("save", "upload-form-changes",
                    "--file-detailed ${context.getWorkDir()}/${RegulationType.UI_FORM.value}")
        } else {
            context.logger.info("Skip ${RegulationType.UI_FORM.value} files deploy due to empty change list")
        }
    }

    private boolean isFormExists(String formName, String token) {
        def response
        try {
            context.logger.debug("Check if form ${formName} already exists using form name")
            response = context.script.httpRequest url: "${FormShemaProvider.PROVIDER_URL}/${formName}",
                    httpMode: 'GET',
                    customHeaders: [[maskValue: true, name: 'X-Access-Token', value: token]],
                    consoleLogResponseBody: context.logLevel == "DEBUG",
                    quiet: context.logLevel != "DEBUG",
                    validResponseCodes: "200,404"
            if (response.getStatus() == 200) {
                context.logger.debug("Form ${formName} exists. Required action is update")
                return true
            } else if (response.getStatus() == 400 && response.getContent().contains("Invalid alias")) {
                context.logger.debug("Form ${formName} does not exist. Required action is create")
                return false
            }
        } catch (any) {
            context.script.error("Form management provider is unavailable")
        }
    }

    private void createForm(String formFile, String content, String token) {
        try {
            context.logger.info("Creating form ${formFile}")
            context.script.httpRequest url: "${FormShemaProvider.PROVIDER_URL}",
                    httpMode: 'POST',
                    requestBody: content,
                    customHeaders: [[maskValue: false, name: 'Content-Type', value: "application/json; charset=utf-8"],
                                    [maskValue: true, name: 'X-Access-Token', value: token]],
                    wrapAsMultipart: false,
                    consoleLogResponseBody: context.logLevel == "DEBUG",
                    quiet: context.logLevel != "DEBUG",
                    validResponseCodes: "201"
            context.logger.info("Form ${formFile} successfully created")
        } catch (any) {
            context.logger.warn("Failed to create form ${formFile}")
        }
    }

    private void updateForm(String formFile, String formName, String content, String token) {
        try {
            context.logger.info("Updating form ${formFile}")
            context.script.httpRequest url: "${FormShemaProvider.PROVIDER_URL}/${formName}",
                    httpMode: 'PUT',
                    requestBody: content,
                    customHeaders: [[maskValue: false, name: 'Content-Type', value: "application/json; charset=utf-8"],
                                    [maskValue: true, name: 'X-Access-Token', value: token]],
                    wrapAsMultipart: false,
                    consoleLogResponseBody: context.logLevel == "DEBUG",
                    quiet: context.logLevel != "DEBUG",
                    validResponseCodes: "200"
            context.logger.info("Form ${formFile} successfully updated")
        } catch (any) {
            context.logger.warn("Failed to update form ${formFile}")
        }
    }

    void deployForm(String formFile, String token) {
        String formJsonContent = context.script.readFile(file: formFile, encoding: "UTF-8")
        String formName = new JsonSlurper().parseText(formJsonContent).name
        if (isFormExists(formName, token)) {
            updateForm(formFile, formName, formJsonContent, token)
        } else {
            createForm(formFile, formJsonContent, token)
        }
    }
}
