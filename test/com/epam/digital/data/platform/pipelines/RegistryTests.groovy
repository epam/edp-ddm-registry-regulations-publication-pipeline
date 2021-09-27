package com.epam.digital.data.platform.pipelines

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.registry.Registry
import com.lesfurets.jenkins.unit.BasePipelineTest
import org.assertj.core.util.Files
import org.junit.Before
import org.junit.Test
import org.yaml.snakeyaml.Yaml
import java.nio.charset.Charset

import static org.junit.Assert.assertEquals

class RegistryTests extends BasePipelineTest {
    Script script
    BuildContext context
    Registry registry

    @Before
    void setUp() {
        super.setUp()
        script = loadScript("vars/Build.groovy")
        context = new BuildContext(script)
        registry = new Registry(context)
        LinkedHashMap settings = new Yaml().load(
                Files.contentOf(
                        new File("test/resources/settings.yaml"), Charset.forName("UTF-8")))
        helper.registerAllowedMethod("readYaml", [Map.class], {
            return settings
        })
    }

    @Test
    void init() throws Exception {
        registry.init()
        registry.toString()
        assertEquals("registry", registry.name)
        assertEquals("1.0.0", registry.version)
    }
}
