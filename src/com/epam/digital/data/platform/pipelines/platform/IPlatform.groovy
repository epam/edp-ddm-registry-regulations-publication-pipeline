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

interface IPlatform {
    String getSecretValue(String secretName, String secretKey)

    String getJsonPathValue(String resourceApi, String resourceName, String jsonPath, String project)

    String podExec(String podName, String command, String container)

    void triggerDeploymentRollout(String deploymentName)

    boolean patchConfigMapKey(String configMap, String key, String value)

    void apply(String filePath)

    void deleteObject(String objectType, String objectName, String parameters, Boolean force)

    boolean checkObjectExists(String objectType, String objectName, String project)

    boolean patch(String resource, String name, String jsonpath)

    void patchByLabel(String resource, String label, String jsonpath)

    void create(String resource, String name, String parameters)

    void annotate(String resource, String name, String key, String value, boolean overwrite)

    void scale(String resource, int replicas)

    String get(String resource, String name, String parameters)

    LinkedHashMap getAsJson(String resource, String name)

    String getAll(String resource, String parameters)

    void addSccToUser(String user, String scc, String project)

    void waitFor(String resource, String condition, String timeout)
}
