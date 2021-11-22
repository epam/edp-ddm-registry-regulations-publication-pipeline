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

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.tools.Logger
import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals

class BuildContextTests extends BasePipelineTest {
    Script script
    BuildContext context

    @Before
    void setUp() {
        super.setUp()
        script = loadScript("vars/Build.groovy")
        context = new BuildContext(script)
        context.logger = new Logger(context.script)
    }

    @Test
    void getLogLevel() throws Exception {
        String expectedLogLevel = "DEBUG"
        script.env["LOG_LEVEL"] = expectedLogLevel
        assertEquals(expectedLogLevel, context.getLogLevel())
    }

    @Test
    void getParameterValue() {
        String paramName = "CODEBASE_NAME"
        String value = "registry-regulations"
        String defaultValue = "registry"
        assertEquals(defaultValue, context.getParameterValue(paramName, defaultValue))
        context.script.env["${paramName}"] = value
        assertEquals(value, context.getParameterValue(paramName, defaultValue))
    }

    @Test
    void workDir() throws Exception {
        String baseDir = "/tmp/test"
        String name = "testWs"
        String fullPath = "${baseDir}/${name}"
        context.initWorkDir()
        context.initWorkDir(baseDir, name)
        assertEquals(fullPath, context.getWorkDir())
        assertEquals("${fullPath}/generated".toString(), context.getGeneratedProjectsDir())
    }
}
