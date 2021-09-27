package com.epam.digital.data.platform.pipelines.buildtool

import com.cloudbees.groovy.cps.NonCPS

enum BuildToolType {
    ANY("any"),
    DOCKER("docker")

    private String value

    BuildToolType(String value) {
        this.value = value
    }

    @NonCPS
    String getValue() {
        return value
    }
}
