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
