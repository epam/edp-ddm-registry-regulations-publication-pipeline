package com.epam.digital.data.platform.pipelines.stages.impl.lowcode

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.FormManagement
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage
import groovy.json.JsonSlurper

@Stage(name = "upload-form-changes", buildTool = ["any"], type = ProjectType.LIBRARY)
class UploadFormChanges {
    BuildContext context

    void run() {
        ArrayList<String> changedFormFiles = context.registryRegulations.filesToDeploy.get(RegulationType.UI_FORM)
        if (changedFormFiles) {
            String token = context.keycloak.getAccessToken(context.jenkinsDeployer)
            changedFormFiles.each {
                deployForm(it, token)
            }
        } else {
            context.logger.info("Skip ${RegulationType.UI_FORM.value} files deploy due to empty change list")
        }
    }

    private boolean isFormExists(String formName, String token) {
        def response
        try {
            context.logger.debug("Check if form ${formName} already exists using form name")
            response = context.script.httpRequest url: "${FormManagement.PROVIDER_URL}/${formName}",
                    httpMode: 'GET',
                    customHeaders: [[maskValue: true, name: 'X-Access-Token', value: token]],
                    consoleLogResponseBody: context.logLevel == "DEBUG",
                    quiet: context.logLevel != "DEBUG",
                    validResponseCodes: "200,400"
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
            context.script.httpRequest url: "${FormManagement.PROVIDER_URL}/form",
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
            context.script.httpRequest url: "${FormManagement.PROVIDER_URL}/${formName}",
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
