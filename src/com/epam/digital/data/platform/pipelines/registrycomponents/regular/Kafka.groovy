/*
 * Copyright 2023 EPAM Systems.
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

package com.epam.digital.data.platform.pipelines.registrycomponents.regular

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class Kafka {
    protected BuildContext context

    public final static String KAFKA_BROKER_POD = "kafka-cluster-kafka-0"
    public final static String KAFKA_BOOTSTRAP_SERVER = "kafka-cluster-kafka-bootstrap:9092"

    ArrayList kafkaTopicList = []
    String kafkaTopics

    Kafka(BuildContext context) {
        this.context = context
    }

    void init() {
        this.kafkaTopicList = getKafkaAllTopicList()
        this.kafkaTopics = getKafkaTopics()
    }

    private ArrayList getKafkaAllTopicList() {
        return context.script.sh(script: "oc exec $KAFKA_BROKER_POD -c kafka -- bin/kafka-topics.sh " +
                "--list --bootstrap-server $KAFKA_BOOTSTRAP_SERVER", returnStdout: true).tokenize('\n')
    }

    private String getKafkaTopics() {
        String kafkaTopicsTmp = ''
        kafkaTopicList.each { kafkaTopic ->
            if ((kafkaTopic.contains("inbound") || kafkaTopic.contains("outbound"))) {
                kafkaTopicsTmp = kafkaTopicsTmp + "$kafkaTopic,"
            }
        }
        return kafkaTopicsTmp
    }

    void removeKafkaTopics() {
        int attempt = 0
        int maxAttempts = 12
        Boolean kafkaTopicsRemoved = false
        while (!kafkaTopicsRemoved) {
            init()
            attempt++
            if (attempt > maxAttempts) {
                context.script.error("Attempts limit is reached and kafka topics were not removed yet!")
                kafkaTopicsRemoved = true
            }
            if (kafkaTopics.length() > 1) {
                try {
                    context.script.sh(script: "oc exec $KAFKA_BROKER_POD -c kafka -- bin/kafka-topics.sh " +
                            "--bootstrap-server $KAFKA_BOOTSTRAP_SERVER --delete --topic ${kafkaTopics.substring(0, kafkaTopics.length() - 1)}")
                } catch (any) {
                    kafkaTopicsRemoved = false
                    context.logger.info("Removing of kafka topics failed. Retrying (attempt $attempt/12)")
                }
            } else {
                kafkaTopicsRemoved = true
                context.logger.info("Kafka topics were successfully removed.")
            }

        }
    }
}
