package com.epam.digital.data.platform.pipelines.platform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class PlatformFactory {
    private final BuildContext context

    PlatformFactory (BuildContext context) {
        this.context = context
    }

    IPlatform getPlatformImpl(final String customPlatformType = null) {
        String platformType = customPlatformType ?: System.getenv("PLATFORM_TYPE")
        if (!platformType)
            context.script.error("Platform type is not defined and " +
                    "environment variable PLATFORM_TYPE is not defined")
        switch (platformType.toLowerCase()) {
            case PlatformType.OPENSHIFT.value:
                return new Openshift(context)
            case PlatformType.KUBERNETES.value:
                return new Kubernetes(context)
            default:
                context.script.error("Failed to determine platform type")
        }
    }
}
