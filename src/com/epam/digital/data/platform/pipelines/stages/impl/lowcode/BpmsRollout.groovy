package com.epam.digital.data.platform.pipelines.stages.impl.lowcode

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.BusinessProcMgmtSys
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "bpms-rollout", buildTool = ["any"], type = ProjectType.LIBRARY)
class BpmsRollout {
    BuildContext context

    void run() {
        if (context.bpmsRestart)
            context.platform.triggerDeploymentRollout(BusinessProcMgmtSys.BPMS_DEPLOYMENT_NAME)
    }
}
