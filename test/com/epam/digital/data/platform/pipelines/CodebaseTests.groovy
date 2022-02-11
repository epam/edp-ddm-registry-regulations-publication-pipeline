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
import com.epam.digital.data.platform.pipelines.codebase.Codebase
import com.epam.digital.data.platform.pipelines.platform.Openshift
import com.epam.digital.data.platform.pipelines.registrycomponents.external.DockerRegistry
import com.epam.digital.data.platform.pipelines.stages.ProjectType
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
        helper.registerAllowedMethod("sh", [Map.class], { cmd ->
            if (cmd.get("script").contains(".spec.type"))
                return "application"

            if (cmd.get("script").contains(".spec.defaultBranch"))
                return "master"

            if (cmd.get("script").contains("-ojson"))
                return "{\"metadata\": {\"name\": \"registry-regulations\"}, \"spec\": " +
                        "{\"buildTool\": \"none\", \"ciTool\": \"Jenkins\", \"commitMessagePattern\": null, " +
                        "\"defaultBranch\": \"master\", \"deploymentScript\": \"\", \"description\": null, " +
                        "\"emptyProject\": false, \"framework\": null, \"gitServer\": \"gerrit\", \"gitUrlPath\": null, " +
                        "\"jenkinsSlave\": \"dataplatform-jenkins-agent\", \"jiraIssueMetadataPayload\": null, " +
                        "\"jobProvisioning\": \"registry\", \"lang\": \"groovy-pipeline\", \"perf\": null, " +
                        "\"repository\": {\"url\": \"https://gerrit/registry-regulations\"}, \"type\": \"library\"}}\n"
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
        if (codebase.type == ProjectType.APPLICATION.getValue()) {
            assertEquals("latest", codebase.getImageTag())
            String customTag = "1.0.0-SNAPSHOT"
            codebase.setImageTag(customTag)
            assertEquals(customTag, codebase.getImageTag())
        }
    }
}
