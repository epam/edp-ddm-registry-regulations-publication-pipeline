package com.epam.digital.data.platform.pipelines.registry

enum BpTrembitaFileType {

    CONFIG("configuration.yml"),
    EXTERNAL_SYSTEM("external-system.yml")


    private String value

    BpTrembitaFileType(String value) {
        this.value = value
    }

    String getValue() {
        return value
    }
}
