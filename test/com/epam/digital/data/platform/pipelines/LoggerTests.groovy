/*
 * Copyright 2021 EPAM Systems.
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

package com.epam.digital.data.platform.pipelines

import com.epam.digital.data.platform.pipelines.tools.Logger
import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.junit.Assert.assertEquals

final class LoggerTests extends BasePipelineTest {
    Script script
    Logger logger

    private final String MESSAGE = "Hello, edp!"

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none()

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        logger = new Logger(script)
    }

    @Test
    void printInfoMessage() {
        String result

        logger.init("INFO")
        result = logger.info(MESSAGE)
        assertEquals("INFO: ${MESSAGE}".toString(), result)

        logger.init("DEBUG")
        result = logger.info(MESSAGE)
        assertEquals("INFO: ${MESSAGE}".toString(), result)

        logger.init("WARN")
        result = logger.info(MESSAGE)
        assertEquals(logger.NO_PRINT_MESSAGE_STRING, result)
    }

    @Test
    void printDebugMessage() {
        String result

        logger.init("DEBUG")
        result = logger.debug(MESSAGE)
        assertEquals("DEBUG: ${MESSAGE}".toString(), result)

        logger.init("WARN")
        result = logger.debug(MESSAGE)
        assertEquals(logger.NO_PRINT_MESSAGE_STRING, result)
    }

    @Test
    void printWarnMessage() {
        String result

        logger.init("WARN")
        result = logger.warn(MESSAGE)
        assertEquals("WARN: ${MESSAGE}".toString(), result)

        logger.init("ERROR")
        result = logger.debug(MESSAGE)
        assertEquals(logger.NO_PRINT_MESSAGE_STRING, result)
    }

    @Test
    void printErrorMessage() {
        String result

        logger.init("ERROR")
        result = logger.error(MESSAGE)
        assertEquals("ERROR: ${MESSAGE}".toString(), result)
    }

    @Test
    void useNonExistingMethod() {
        exceptionRule.expect(GroovyRuntimeException)
        exceptionRule.expectMessage("Could not find matching constructor for: " +
                "groovy.lang.MissingMethodException(java.lang.String)")
        logger.init("DEBUG")
        logger.trace(MESSAGE)
    }

    @Test
    void useNonExistingLogLevel() {
        exceptionRule.expect(IllegalArgumentException)
        exceptionRule.expectMessage("No enum constant " +
                "com.epam.digital.data.platform.pipelines.tools.LogLevel.TRACE")
        logger.init("TRACE")
    }
}
