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

enum RegulationType {
    BUSINESS_PROCESS("bpmn"),
    BUSINESS_RULE("dmn"),
    UI_FORM("forms"),
    GLOBAL_VARS("global-vars"),
    DATA_MODEL("data-model"),
    REPORTS("reports"),
    ROLES("roles"),
    BUSINESS_PROCESS_AUTH("bp-auth"),
    BUSINESS_PROCESS_TREMBITA("bp-trembita"),
    EXCERPTS("excerpts"),
    NOTIFICATION_TEMPLATES("notifications"),
    REGISTRY_SETTINGS("settings"),
    BP_GROUPING("bp-grouping")

    private String value

    RegulationType(String value) {
        this.value = value
    }

    String getValue() {
        return value
    }
}
