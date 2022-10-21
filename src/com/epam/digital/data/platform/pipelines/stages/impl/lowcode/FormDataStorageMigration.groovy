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
import com.epam.digital.data.platform.pipelines.helper.DecodeHelper
import com.epam.digital.data.platform.pipelines.registrycomponents.external.CephBucket
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.Redis
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "form-data-storage-migration", buildTool = ["any"], type = [ProjectType.LIBRARY])
class FormDataStorageMigration {
    BuildContext context
    private final String MIGRATOR_JAR = "/home/jenkins/form-data-storage-migration-cli/form-data-storage-migration-cli.jar"

    void run() {
        String migrationCM = "form-data-storage-migration"
        if (context.platform.checkObjectExists("cm", migrationCM)) {
            context.logger.info("Skip form data storage migration because it is already migrated")
        } else {
            CephBucket formDataStorageBucket = new CephBucket("lowcode-form-data-storage", context)
            formDataStorageBucket.init()
            try {
                context.logger.info("Migrating form data storage from OBC to redis")
                LinkedHashMap redisSecretJson = context.platform.getAsJson("secret", Redis.REDIS_SECRET)["data"]
                String password = DecodeHelper.decodeBase64(redisSecretJson["password"])

                String additionalKeyPatterns = context.getParameterValue("ADDITIONAL_KEY_PATTERNS", "")
                String migratorParams = "" +
                        "--delete-invalid-data=" +
                        "${context.getParameterValue("DELETE_INVALID_DATA", "false").toBoolean()} " +
                        "--delete-after-migration=" +
                        "${context.getParameterValue("DELETE_AFTER_MIGRATION", "false").toBoolean()} " +
                        "--s3.config.client.protocol=http " +
                        "--s3.config.options.pathStyleAccess=true " +
                        "--storage.backend.redis.sentinel.nodes=${Redis.REDIS_NODES} " +
                        "--storage.backend.redis.sentinel.master=${Redis.REDIS_MASTER} " +
                        "--storage.backend.redis.password=${password} " +
                        "--storage.backend.ceph.bucket=${formDataStorageBucket.cephBucketName} " +
                        "--storage.backend.ceph.http-endpoint=${formDataStorageBucket.cephHttpEndpoint} " +
                        "--storage.backend.ceph.access-key=${formDataStorageBucket.cephAccessKey} " +
                        "--storage.backend.ceph.secret-key=${formDataStorageBucket.cephSecretKey} "
                if (additionalKeyPatterns) {
                    migratorParams += "--additional-key-patterns=${additionalKeyPatterns} "
                }

                context.script.sh(script: "set +x; java -jar " +
                        "${MIGRATOR_JAR} " +
                        "${migratorParams} " +
                        "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
                context.logger.info("Form data storage has been successfully migrated")
                context.platform.create("cm", migrationCM)
            } catch (any) {
                context.script.error("Failed to migrate form data storage")
            }
        }
    }
}
