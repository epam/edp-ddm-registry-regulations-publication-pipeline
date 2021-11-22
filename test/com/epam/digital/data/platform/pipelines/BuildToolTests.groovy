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
