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
import com.epam.digital.data.platform.pipelines.registry.RegistryRegulations
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.tools.Logger
import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class RegistryRegulationTests extends BasePipelineTest {
    Script script
    BuildContext context
    RegistryRegulations registryRegulations

    @Before
    void setUp() {
        super.setUp()
        script = loadScript("vars/Build.groovy")
        context = new BuildContext(script)
        context.logger = new Logger(context.script)
        registryRegulations = new RegistryRegulations(context)
    }

    private void mockSh(String result) {
        helper.registerAllowedMethod("sh", [String.class], { cmd ->
            return result
        })
        helper.registerAllowedMethod("sh", [Map.class], { cmd ->
            return result
        })
    }

    @Test
    void getAllRegulationsTest() throws Exception {
        helper.registerAllowedMethod('sh', [Map.class], { cmd ->
            if (cmd.get("script").contains("find"))
                return "file1\nfile2\nfile3"
        })
        assertEquals(registryRegulations.getAllRegulations(RegulationType.ROLES), ["file1", "file2", "file3"])
    }

    @Test
    void getRegistryConfValuesTest() throws Exception {
        helper.registerAllowedMethod('sh', [Map.class], { cmd ->
            if (cmd.get("script").contains("deployProfile"))
                return "development"
        })
        assertEquals(registryRegulations.getRegistryConfValues(true), "development")
        String path = context.initWorkDir()
        assertTrue(registryRegulations.getRegistryConfValues(false).contains("platform-values.yaml"))
    }

    @Test
    void getChangedStatusOrFilesTest() throws Exception {
        helper.registerAllowedMethod('sh', [Map.class], { cmd ->
            if (cmd.get("script").contains("java -jar"))
                return "PlanCommandExecutionStart role_1,role_2 PlanCommandExecutionEnd"
        })
        assertEquals(registryRegulations.getChangedStatusOrFiles("plan", "create-keycloak-roles", "--file-detailed roles"), ["role_1", "role_2"])
        context.script.env["FULL_DEPLOY"] = true
        assertEquals(registryRegulations.getChangedStatusOrFiles("plan", "redash-roles", "--file"), ["fullDeploy"])
    }

    @Test
    void deployStatusTest() throws Exception {
        helper.registerAllowedMethod('sh', [Map.class], { cmd ->
            if (cmd.get("script").contains("plan"))
                return "PlanCommandExecutionStart true PlanCommandExecutionEnd"
        })
        String path = context.initWorkDir()
        assertEquals(registryRegulations.deployStatus("create-keycloak-roles", "roles"), true)
        helper.registerAllowedMethod('sh', [Map.class], { cmd ->
            if (cmd.get("script").contains("plan"))
                return "PlanCommandExecutionStart false PlanCommandExecutionEnd"
        })
        assertEquals(registryRegulations.deployStatus("create-keycloak-roles", "roles"), false)
    }
}
