package com.epam.digital.data.platform.pipelines.stages

import com.cloudbees.groovy.cps.NonCPS

enum ProjectType {
    APPLICATION("application"),
    LIBRARY("library")

    private String value

    ProjectType(String value) {
        this.value = value
    }

    @NonCPS
    String getValue() {
        return value
    }
}
