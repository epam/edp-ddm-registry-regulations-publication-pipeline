package com.epam.digital.data.platform.pipelines

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.platform.*
import com.epam.digital.data.platform.pipelines.tools.Logger
import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.junit.Assert.assertEquals

class PlatformTests extends BasePipelineTest {
    Script script
    BuildContext context
    IPlatform platform
    private final String MOCK_ARG = "mockarg"
    private final String MOCK_RESULT = "mockresult"

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none()

    @Before
    void setUp() {
        super.setUp()
        script = loadScript("vars/Build.groovy")
        context = new BuildContext(script)
        context.logger = new Logger(context.script)
        helper.registerAllowedMethod("error", [String.class], { cmd ->
            throw new GroovyRuntimeException(cmd)
        })
    }

    @Test
    void shouldBeOpenshift() throws Exception {
        platform = new PlatformFactory(context).getPlatformImpl("openshift")
        assertEquals(Openshift.class, platform.class)
    }

    @Test
    void shouldBeKubernetes() throws Exception {
        platform = new PlatformFactory(context).getPlatformImpl("kubernetes")
        assertEquals(Kubernetes.class, platform.class)
    }

    @Test
    void notSupportedPlatform() throws Exception {
        exceptionRule.expect(GroovyRuntimeException)
        exceptionRule.expectMessage("Failed to determine platform type")
        platform = new PlatformFactory(context).getPlatformImpl("kubernetes1")
    }

    @Test
    void platformNotSet() throws Exception {
        exceptionRule.expect(GroovyRuntimeException)
        exceptionRule.expectMessage("Platform type is not defined and " +
                "environment variable PLATFORM_TYPE is not defined")
        platform = new PlatformFactory(context).getPlatformImpl()
    }

    @Test
    void getValue() throws Exception {
        assertEquals("openshift", PlatformType.OPENSHIFT.getValue())
    }

    @Test
    void checkObjectExists() throws Exception {
        platform = new PlatformFactory(context).getPlatformImpl("kubernetes")
        mockSh(MOCK_RESULT)
        assertEquals(true, platform.checkObjectExists(MOCK_ARG, MOCK_ARG))
        assertEquals(true, platform.checkObjectExists(MOCK_ARG, MOCK_ARG, MOCK_ARG))
        /* If empty result returns */
        mockSh("")
        assertEquals(false, platform.checkObjectExists(MOCK_ARG, MOCK_ARG))
    }

    @Test
    void patch() throws Exception {
        platform = new PlatformFactory(context).getPlatformImpl("kubernetes")
        mockSh("patched")
        assertEquals(true, platform.patch(MOCK_ARG, MOCK_ARG, MOCK_ARG))
        assertEquals(true, platform.patchConfigMapKey(MOCK_ARG, MOCK_ARG, MOCK_ARG))

        mockSh("not patched")
        assertEquals(false, platform.patch(MOCK_ARG, MOCK_ARG, MOCK_ARG))
        assertEquals(false, platform.patchConfigMapKey(MOCK_ARG, MOCK_ARG, MOCK_ARG))
    }

    @Test
    void mockCalls() throws Exception {
        mockSh(MOCK_RESULT)

        /* Openshift methods */
        platform = new PlatformFactory(context).getPlatformImpl("openshift")
        platform.addSccToUser(MOCK_ARG, MOCK_ARG, MOCK_ARG)

        /* Kubernetes methods */
        /* void methods */
        platform = new PlatformFactory(context).getPlatformImpl("kubernetes")
        platform.addSccToUser(MOCK_ARG, MOCK_ARG, MOCK_ARG)
        platform.triggerDeploymentRollout(MOCK_ARG)
        platform.create(MOCK_ARG, MOCK_ARG)
        platform.create(MOCK_ARG, MOCK_ARG, MOCK_ARG)
        platform.apply(MOCK_ARG)
        platform.annotate(MOCK_ARG, MOCK_ARG, MOCK_ARG, MOCK_ARG)
        platform.annotate(MOCK_ARG, MOCK_ARG, MOCK_ARG, MOCK_ARG, true)
        platform.deleteObject(MOCK_ARG, MOCK_ARG)
        platform.deleteObject(MOCK_ARG, MOCK_ARG, true)
        platform.scale(MOCK_ARG, 1)
        platform.waitFor(MOCK_ARG, MOCK_ARG, MOCK_ARG)

        /* String methods */
        assertEquals(MOCK_RESULT, platform.get(MOCK_ARG, MOCK_ARG))
        assertEquals(MOCK_RESULT, platform.get(MOCK_ARG, MOCK_ARG, MOCK_ARG))
        assertEquals(MOCK_RESULT, platform.getAll(MOCK_ARG))
        assertEquals(MOCK_RESULT, platform.getAll(MOCK_ARG, MOCK_ARG))
        assertEquals(MOCK_RESULT, platform.getJsonPathValue(MOCK_ARG, MOCK_ARG, MOCK_ARG))
        assertEquals(MOCK_RESULT, platform.getJsonPathValue(MOCK_ARG, MOCK_ARG, MOCK_ARG, MOCK_ARG))
        assertEquals(MOCK_RESULT, platform.podExec(MOCK_ARG, MOCK_ARG))
        assertEquals(MOCK_RESULT, platform.podExec(MOCK_ARG, MOCK_ARG, MOCK_ARG))

        mockSh(MOCK_RESULT.bytes.encodeBase64().toString())
        assertEquals(MOCK_RESULT, platform.getSecretValue(MOCK_ARG, MOCK_ARG))
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
