package com.epam.digital.data.platform.pipelines.buildtool

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class BuildToolFactory {
    static IBuildTool getBuildToolImpl(String buildTool, BuildContext context) {
        switch (buildTool) {
            case BuildToolType.DOCKER.getValue():
                return new Docker(context)
            default:
                return new Any(context)
        }
    }
}
