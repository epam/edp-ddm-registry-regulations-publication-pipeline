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

package com.epam.digital.data.platform.pipelines.platform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class PlatformFactory {
    private final BuildContext context

    PlatformFactory(BuildContext context) {
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
