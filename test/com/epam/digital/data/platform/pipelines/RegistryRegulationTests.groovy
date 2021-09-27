package com.epam.digital.data.platform.pipelines

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.RegistryRegulations
import com.epam.digital.data.platform.pipelines.registry.RegulationType
import com.epam.digital.data.platform.pipelines.tools.Logger
import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals

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

    @Test
    void getChangedRegulations() throws Exception {
        mockSh(null)
        assertEquals([], registryRegulations.getChangedRegulations(RegulationType.BUSINESS_PROCESS))
        mockSh("bpms/test1.xml\n" +
                "bpms/test2.xml")
        assertEquals(["bpms/test1.xml", "bpms/test2.xml"],
                registryRegulations.getChangedRegulations(RegulationType.BUSINESS_PROCESS))

        context.script.env["FULL_DEPLOY"] = "true"
        assertEquals(["bpms/test1.xml", "bpms/test2.xml"],
                registryRegulations.getChangedRegulations(RegulationType.BUSINESS_PROCESS))
        mockSh(null)
        assertEquals([], registryRegulations.getChangedRegulations(RegulationType.BUSINESS_PROCESS))
    }

    private void mockSh(String result) {
        helper.registerAllowedMethod("sh", [String.class], { cmd ->
            return result
        })
        helper.registerAllowedMethod("sh", [Map.class], { cmd ->
            return result
        })
    }
}
