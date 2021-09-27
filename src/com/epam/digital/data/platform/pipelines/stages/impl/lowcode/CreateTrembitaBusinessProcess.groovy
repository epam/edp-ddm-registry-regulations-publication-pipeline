package com.epam.digital.data.platform.pipelines.stages.impl.lowcode

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "create-trembita-business-process", buildTool = ["any"], type = ProjectType.LIBRARY)
class CreateTrembitaBusinessProcess {
    BuildContext context

    void run() {
        try {
            String BP_TREMBITA_FILE = "${RegulationType.BUSINESS_PROCESS_TREMBITA.value}/external-system.yml"
            String value = context.script.sh(script: """awk '{printf "%s%s\\n", "", \$0}' ${BP_TREMBITA_FILE}""",
                    returnStdout: true).replaceAll("\n", "\\\\n")
            boolean patched = context.platform.patchConfigMapKey(
                    "bp-webservice-gateway-trembita-business-processes", "trembita-business-processes.yml", value)
            if (patched)
                context.platform.triggerDeploymentRollout("bp-webservice-gateway")
        }

        catch (any) {
            context.logger.error("Error during uploading trembita business process changes")
            context.stageFactory.runStage(context.RESTORE_STAGE, context)
        }
    }
}
