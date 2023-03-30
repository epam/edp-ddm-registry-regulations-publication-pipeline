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

package com.epam.digital.data.platform.pipelines

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.platform.Openshift
import com.epam.digital.data.platform.pipelines.registry.CleanupRegistryRegulation
import com.epam.digital.data.platform.pipelines.registrycomponents.external.DockerRegistry
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.Gerrit
import com.epam.digital.data.platform.pipelines.tools.Logger
import com.lesfurets.jenkins.unit.BasePipelineTest
import org.assertj.core.util.Files
import org.junit.Before
import org.junit.Test
import org.yaml.snakeyaml.Yaml

import java.nio.charset.Charset

import static org.junit.Assert.assertEquals

class CleanupRegistryRegulationTests extends BasePipelineTest {
    Script script
    BuildContext context

    @Before
    void init() {
        super.setUp()
        def script = loadScript("vars/Cleanup.groovy")
        helper.registerAllowedMethod("sshagent", [List.class, Closure.class], { list, closure ->
            sshAgentList = list
            return closure()
        })
        context = new BuildContext(script)
        context.cleanup = new CleanupRegistryRegulation(context)
        context.platform = new Openshift(context)
        context.logger = new Logger(context.script)
        context.dockerRegistry = new DockerRegistry(context)
        context.gitServer = new Gerrit(context, "gerrit")

        LinkedHashMap registryRegulationsCm = new Yaml().load(
                Files.contentOf(
                        new File("test/resources/registry-regulations-configmap.yaml"), Charset.forName("UTF-8")))
        helper.registerAllowedMethod("readYaml", [Map.class], {
            return registryRegulationsCm
        })
        helper.registerAllowedMethod("writeYaml", [Map.class], {
            return registryRegulationsCm
        })
        helper.registerAllowedMethod('httpRequest', [Map.class], {
            return ["status": 200, "content": "{\"lastRunResult\": \"OK\", \"currentState\": \"WAITING\"}"] })

    }

    @Test
    void createResourceTest() throws Exception {
        helper.registerAllowedMethod('sh', [Map.class], { cmd ->
            if (cmd.get("script").contains("patch deployment"))
                return true
        })
        assertEquals(context.cleanup.createResource("registry-regulations"), null)
    }

    @Test
    void createTempSecretTest() throws Exception {
        assertEquals(context.cleanup.createTempSecret("registry-regulations"), null)
        helper.registerAllowedMethod('sh', [Map.class], { cmd ->
            if (cmd.get("script").contains("--ignore-not-found=true"))
                return ''
            if (cmd.get("script").contains("get secret"))
                return "bW9jaw=="
        })
        assertEquals(context.cleanup.createTempSecret("registry-regulations"), null)

    }

    @Test
    void recreateDefaultCodebaseRelatedResourcesTest() throws Exception {
        helper.registerAllowedMethod('sh', [Map.class], { cmd ->
            if (cmd.get("script").contains("gerrit -ojson"))
                return "{\"metadata\": {\"name\": \"gerrit\"}, \"spec\": {\"nameSshKeySecret\": \"gerrit-test-sshkey\"," +
                        " \"gitUser\": \"jenkins\", \"gitHost\": \"gerrit\", \"sshPort\": \"31000\"}}"
            if (cmd.get("script").contains("get codebase"))
                return ""
            if (cmd.get("script").contains("get codebasebranch"))
                return ""
            if (cmd.get("script").contains("ssh"))
                return "registry-regulations"
            if (cmd.get("script").contains("--ignore-not-found=true"))
                return "registry-regulations"
            if (cmd.get("script").contains("get secret"))
                return "bW9jaw=="
            if (cmd.get("script").contains("patch deployment"))
                return true
        })
        context.gitServer.init()
        assertEquals(context.cleanup.recreateDefaultCodebaseRelatedResources( "registry-regulations"), null)
        helper.registerAllowedMethod('sh', [Map.class], { cmd ->
            if (cmd.get("script").contains("gerrit -ojson"))
                return "{\"metadata\": {\"name\": \"gerrit\"}, \"spec\": {\"nameSshKeySecret\": \"gerrit-test-sshkey\"," +
                        " \"gitUser\": \"jenkins\", \"gitHost\": \"gerrit\", \"sshPort\": \"31000\"}}"
            if (cmd.get("script").contains("get codebase"))
                return "registry-regulations"
            if (cmd.get("script").contains("get codebasebranch"))
                return ""
            if (cmd.get("script").contains("--ignore-not-found=true"))
                return ""
            if (cmd.get("script").contains("ssh"))
                return "registry-regulations"
        })
        context.gitServer.init()
        assertEquals(context.cleanup.recreateDefaultCodebaseRelatedResources( "registry-regulations"), null)

        helper.registerAllowedMethod('sh', [Map.class], { cmd ->
            if (cmd.get("script").contains("gerrit -ojson"))
                return "{\"metadata\": {\"name\": \"gerrit\"}, \"spec\": {\"nameSshKeySecret\": \"gerrit-test-sshkey\"," +
                        " \"gitUser\": \"jenkins\", \"gitHost\": \"gerrit\", \"sshPort\": \"31000\"}}"
            if (cmd.get("script").contains("get codebase"))
                return "deletionTimestamp"
            if (cmd.get("script").contains("get codebasebranch"))
                return "deletionTimestamp"
            if (cmd.get("script").contains("ssh"))
                return ""
            if (cmd.get("script").contains("get gerritproject"))
                return "mock"
        })
        context.gitServer.init()
        assertEquals(context.cleanup.recreateDefaultCodebaseRelatedResources( "registry-regulations"), null)
    }


    @Test
    void triggerManualNexusTask() throws Exception {
        assertEquals(context.cleanup.triggerManualNexusTask("docker-delete-unused-manifest-and-tags", true), null)
        helper.registerAllowedMethod('httpRequest', [Map.class], {
            return ["status": 404, "content": "{\"lastRunResult\": \"OK\", \"currentState\": \"WAITING\"}"] })
        assertEquals(context.cleanup.triggerManualNexusTask("docker-delete-unused-manifest-and-tags", true), null)
    }

    @Test
    void removeFinalizersTest() throws Exception {
        assertEquals(context.cleanup.removeFinalizers("registry-regulations"), null)
        helper.registerAllowedMethod('sh', [Map.class], { cmd ->
            if (cmd.get("script").contains("-o json"))
                return "deletionTimestamp"
            if (cmd.get("script").contains("--type merge"))
                return true
        })
        assertEquals(context.cleanup.removeFinalizers("registry-regulations"), null)
    }

    @Test
    void checkFinalizersTest() throws Exception {
        helper.registerAllowedMethod('sh', [Map.class], { cmd ->
            println(cmd)
            if (cmd.get("script").contains("-o json"))
                return true
        })
        assertEquals(context.cleanup.removeFinalizers("registry-regulations"), null)
        helper.registerAllowedMethod('sh', [Map.class], { cmd ->
            println(cmd)
            if (cmd.get("script").contains("-o json"))
                return false
        })
        assertEquals(context.cleanup.checkFinalizers("registry-regulations"), null)
    }

    @Test
    void checkRepositoryTest() throws Exception {
        helper.registerAllowedMethod('sh', [Map.class], { cmd ->
            if (cmd.get("script").contains("ssh"))
                return "registry-regulations"
        })
        assertEquals(context.cleanup.checkRepository("registry-regulations"), null)
        helper.registerAllowedMethod('sh', [Map.class], { cmd ->
            if (cmd.get("script").contains("ssh"))
                return ""
            if (cmd.get("script").contains("--type merge"))
                return "registry-regulations"
            if (cmd.get("script").contains("gerritproject"))
                return ""

        })
        assertEquals(context.cleanup.checkRepository("registry-regulations"), null)
    }
}
