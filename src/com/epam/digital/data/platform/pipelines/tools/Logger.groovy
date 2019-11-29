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

package com.epam.digital.data.platform.pipelines.tools

class Logger {
    private final Script script
    private int logLevel

    public final String NO_PRINT_MESSAGE_STRING = "No message was printed"

    Logger(Script script) {
        this.script = script
        this.logLevel = 2
    }

    void init(String logLevel = "INFO") {
        this.logLevel = LogLevel.valueOf(logLevel.toUpperCase()).getValue()
    }

    private String printMessage(final String message, LogLevel messageLogLevel) {
        if (messageLogLevel.getValue() <= logLevel) {
            String messageToPrint = "${messageLogLevel.name()}: ${message}"
            script.println(messageToPrint)
            return messageToPrint
        } else {
            return NO_PRINT_MESSAGE_STRING
        }
    }

    String methodMissing(String name, def args) {
        String message = args[0]
        switch (name.toLowerCase()) {
            case "debug":
                printMessage(message, LogLevel.DEBUG)
                break
            case "info":
                printMessage(message, LogLevel.INFO)
                break
            case "warn":
                printMessage(message, LogLevel.WARN)
                break
            case "error":
                printMessage(message, LogLevel.ERROR)
                break
            default:
                throw new MissingMethodException(name)
        }
    }
}
