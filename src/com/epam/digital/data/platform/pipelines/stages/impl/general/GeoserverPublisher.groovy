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
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "publish-geoserver-configuration", buildTool = ["any"], type = [ProjectType.LIBRARY])
class GeoserverPublisher {
    BuildContext context

    private final String GEOSERVER_PUBLISHER_JAR = "/home/jenkins/geoserver-publisher/geoserver-publisher.jar"

    void run() {
        if (context.platform.checkObjectExists("secret", "geo-server-admin-secret")) {
            context.logger.info("Run geo-server publisher")
            runGeoserverPublisher()
        } else {
            context.logger.info("Skip geo-server publisher due to no geo-server in registry")
        }
    }

    private void runGeoserverPublisher() {
        context.logger.info("Publishing geoserver configuration")
        context.script.sh(script: "cp ${context.registry.SETTINGS_FILE} /home/jenkins/geoserver-publisher")
        try {
            context.script.sh(script: "java -jar " +
                    "-DSTORE_DB_PASSWORD=${context.postgres.geoServerPublisherPass} " +
                    "-DSTORE_DB_USER=${context.postgres.geoServerPublisherUser} " +
                    "-DDB_SCHEMA=registry " +
                    "-DDB_HOST=${context.postgres.OPERATIONAL_MASTER_URL} " +
                    "-DDB_PORT=${context.postgres.OPERATIONAL_MASTER_PORT} " +
                    "-DDB_NAME=registry " +
                    "-DGEOSERVER_LOGIN=admin " +
                    "-DGEOSERVER_PASSWORD=${context.platform.getSecretValue("geo-server-admin-secret","password")} " +
                    "-DGEOSERVER_PUBLISHER_DB_PASSWORD=\'${context.postgres.operational_pg_password}\' " +
                    "-DGEOSERVER_PUBLISHER_DB_USER=${context.postgres.operational_pg_user} " +
                    "-DGEOSERVER_URL=http://officer-portal-geo-server:8080 " +
                    "${GEOSERVER_PUBLISHER_JAR} " +
                    "--settings-file=${context.registry.SETTINGS_FILE} " +
                    "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
            context.logger.info("Geoserver configuration has been published")
        }
        catch (any) {
            context.script.error("Geoserver configuration publishing failed")
        }
    }
}
