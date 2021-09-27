package com.epam.digital.data.platform.pipelines

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.codebase.Codebase
import com.epam.digital.data.platform.pipelines.platform.Openshift
import com.epam.digital.data.platform.pipelines.registrycomponents.external.DockerRegistry
import com.epam.digital.data.platform.pipelines.tools.Logger
import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals

class CodebaseTests extends BasePipelineTest {
    Script script
    BuildContext context
    Codebase codebase

    @Before
    void setUp() {
        super.setUp()
        script = loadScript("vars/Build.groovy")
        helper.registerAllowedMethod("sh", [Map.class], {cmd->
            if (cmd.get("script").contains(".spec.type"))
                return "application"

            if (cmd.get("script").contains(".spec.defaultBranch"))
                return "master"
        })

        context = new BuildContext(script)
        context.logger = new Logger(context.script)
        context.platform = new Openshift(context)
        context.dockerRegistry = new DockerRegistry(context)
        context.dockerRegistry.host = "mock"
        codebase = new Codebase(context)
        codebase.init()
        codebase.toString()
        codebase.initBuildTool()
    }

    @Test
    void getImageTag() throws Exception {
        assertEquals("latest", codebase.getImageTag())
        String customTag = "1.0.0-SNAPSHOT"
        codebase.setImageTag(customTag)
        assertEquals(customTag, codebase.getImageTag())
    }
}
