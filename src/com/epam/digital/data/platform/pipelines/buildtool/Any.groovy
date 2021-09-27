package com.epam.digital.data.platform.pipelines.buildtool

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class Any implements IBuildTool {
    private final BuildContext context

    Any(BuildContext context) {
        this.context = context
    }

    @Override
    void init() {
        context.logger.debug("Init Any build tool")
    }
}
