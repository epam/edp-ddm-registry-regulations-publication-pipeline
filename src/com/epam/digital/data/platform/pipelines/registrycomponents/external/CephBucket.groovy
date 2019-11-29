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

package com.epam.digital.data.platform.pipelines.registrycomponents.external

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class CephBucket {
    private final BuildContext context
    public String name
    public String cephBucketName
    public String cephHttpEndpoint
    public String cephAccessKey
    public String cephSecretKey

    CephBucket(String name, BuildContext context) {
        this.name = name
        this.context = context
    }

    void init() {
        this.cephBucketName = context.platform.getJsonPathValue("configmap", name, ".data.BUCKET_NAME")
        this.cephHttpEndpoint = context.platform.getJsonPathValue("configmap", name, ".data.BUCKET_HOST")
        this.cephAccessKey = context.platform.getSecretValue(name, "AWS_ACCESS_KEY_ID")
        this.cephSecretKey = context.platform.getSecretValue(name, "AWS_SECRET_ACCESS_KEY")
    }
}
