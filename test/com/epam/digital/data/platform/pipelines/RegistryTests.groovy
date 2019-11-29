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
