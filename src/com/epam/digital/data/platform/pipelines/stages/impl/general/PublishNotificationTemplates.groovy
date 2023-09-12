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

package com.epam.digital.data.platform.pipelines.stages.impl.general

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.NotificationService
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "publish-notification-templates", buildTool = ["any"], type = [ProjectType.LIBRARY])
class PublishNotificationTemplates {
    BuildContext context

    private final String NOTIFICATION_TEMPLATES_PUBLISHER_JAR = "/home/jenkins/notification-template-publisher/notification-template-publisher.jar"

    void run() {
        if (context.registryRegulations.deployStatus("publish-notification-templates",
                "${RegulationType.NOTIFICATION_TEMPLATES.value}")) {
            context.logger.info("Publish notification templates")
            publishNotificationTemplates()
            context.registryRegulations.getChangedStatusOrFiles("save", "publish-notification-templates",
                    "--file ${context.getWorkDir()}/${RegulationType.NOTIFICATION_TEMPLATES.value}")
        } else {
            context.logger.info("Skip notification templates publishing due to no changes")
        }
    }

    private void publishNotificationTemplates() {
        context.logger.info("Publishing of notification templates")
        try {
            context.script.sh(script: "java -jar " +
                    "${NOTIFICATION_TEMPLATES_PUBLISHER_JAR} " +
                    "--notification-service.url=${NotificationService.URL} " +
                    "--thirdPartySystems.accessToken=${context.keycloak.getAccessToken(context.jenkinsDeployer)} " +
                    "--notification_templates " +
                    "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
            context.logger.info("Notification templates have been published")
        }
        catch (any) {
            context.script.error("Notification templates publishing failed")
        }
    }
}
