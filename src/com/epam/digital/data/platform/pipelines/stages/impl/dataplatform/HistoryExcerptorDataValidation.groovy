package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "data-validation", buildTool = "any", type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class HistoryExcerptorDataValidation {
    BuildContext context

    void run() {

        context.logger.info("Validating requested table name")
        if (!context.citus.psqlCommand(context.citus.masterRepPod,
                "select count(*) from pg_tables where tablename='${context.script.env.NAME_OF_TABLE}' " +
                        "and schemaname='public';", "registry").trim().toBoolean()) {
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