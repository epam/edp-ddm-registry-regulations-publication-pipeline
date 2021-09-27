package com.epam.digital.data.platform.pipelines

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.buildtool.*
import com.epam.digital.data.platform.pipelines.tools.Logger
import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals

class BuildToolTests extends BasePipelineTest {
    Script script
    BuildContext context
    IBuildTool buildTool

    @Before
    void setUp() {
        super.setUp()
        script = loadScript("vars/Build.groovy")
        context = new BuildContext(script)
        context.logger = new Logger(context.script)
    }

    @Test
    void shouldBeAny() throws Exception {
        String buildToolSpec = "none"
        buildTool = BuildToolFactory.getBuildToolImpl(buildToolSpec, context)
        assertEquals(Any.class, buildTool.class)
    }

    @Test
    void shouldBeDocker() throws Exception {
        String buildToolSpec = "docker"
        buildTool = BuildToolFactory.getBuildToolImpl(buildToolSpec, context)
        assertEquals(Docker.class, buildTool.class)
    }

    @Test
    void getValue() throws Exception {
        assertEquals("docker", BuildToolType.DOCKER.getValue())
    }
}
