package com.epam.digital.data.platform.pipelines.buildtool

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class Docker implements IBuildTool {
    private final BuildContext context

    Docker(BuildContext context) {
        this.context = context
    }

    @Override
    void init() {
        context.logger.debug("Init Docker build tool")
    }
}
