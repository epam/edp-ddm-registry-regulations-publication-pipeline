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
    EXCERPTS("excerpts")

    private String value

    RegulationType(String value) {
        this.value = value
    }

    String getValue() {
        return value
    }
}
