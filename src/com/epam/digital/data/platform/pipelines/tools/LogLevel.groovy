package com.epam.digital.data.platform.pipelines.tools

enum LogLevel {
    ERROR(0),
    WARN(1),
    INFO(2),
    DEBUG(3)

    private int value

    LogLevel(int value) {
        this.value = value
    }

    int getValue() {
        return value
    }
}
