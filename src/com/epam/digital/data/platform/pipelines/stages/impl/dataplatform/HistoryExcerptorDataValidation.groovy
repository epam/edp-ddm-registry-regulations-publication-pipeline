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

package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "data-validation", buildTool = "any", type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class HistoryExcerptorDataValidation {
    BuildContext context

    void run() {

        context.logger.info("Validating requested table name")
        String currentSchemaName = context.citus.getCurrentSchema(context.citus.masterRepPod, "registry").trim()
        if (!context.citus.psqlCommand(context.citus.masterRepPod,
                "select count(*) from pg_tables where tablename='${context.script.env.NAME_OF_TABLE}' " +
                        "and schemaname='${currentSchemaName}';", "registry").trim().toBoolean()) {
            context.script.error("Table ${context.script.env.NAME_OF_TABLE} does'nt exist. Validadion FAILED")
        }
        context.logger.info("Table name validation passed")
        try {
            UUID uuid = UUID.fromString(context.script.env.ID);
            context.logger.info("Requested ID is VALID: ${uuid}")
            context.logger.info("UUID validation passed")
        } catch (IllegalArgumentException exception) {
            context.script.error("Please enter ID in UUID format xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx. " +
                    "Validadion FAILED")
        }
    }
}