package com.epam.digital.data.platform.pipelines.platform

enum PlatformType {
    OPENSHIFT("openshift"), KUBERNETES("kubernetes")

    private String value

    PlatformType(String value) {
        this.value = value
    }

    String getValue() {
        return value
    }
}
