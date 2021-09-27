package com.epam.digital.data.platform.pipelines.registrycomponents.generated

enum DataComponentType {
    KAFKA_API("kafka-api"),
    SOAP_API("soap-api"),
    REST_API("rest-api"),
    MODEL("model")

    private String value

    DataComponentType(String value) {
        this.value = value
    }

    String getValue() {
        return value
    }
}
