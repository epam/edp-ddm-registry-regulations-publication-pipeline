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

package com.epam.digital.data.platform.pipelines.registry

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class Registry {

    public String name
    public String version
    public String SETTINGS_FILE = "settings.yaml"
    public String REGISTRY_SETTINGS_FILE = "settings.yml"
    public String REGISTRY_SETTINGS_FILE_PATH

    private final BuildContext context

    Registry(BuildContext context) {
        this.context = context
    }

    void init() {
        LinkedHashMap settingsYaml = context.script.readYaml file: SETTINGS_FILE
        this.name = settingsYaml["settings"]["general"]["register"]
        this.version = settingsYaml["settings"]["general"]["version"]
        REGISTRY_SETTINGS_FILE_PATH = "${RegulationType.REGISTRY_SETTINGS.getValue()}/${REGISTRY_SETTINGS_FILE}"
    }

    @Override
    String toString() {
        return "Registry{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                '}'
    }
}
