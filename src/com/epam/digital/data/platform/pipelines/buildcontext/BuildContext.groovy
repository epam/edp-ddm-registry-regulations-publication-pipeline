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

package com.epam.digital.data.platform.pipelines.buildcontext

import com.epam.digital.data.platform.pipelines.codebase.Codebase
import com.epam.digital.data.platform.pipelines.platform.IPlatform
import com.epam.digital.data.platform.pipelines.registry.Registry
import com.epam.digital.data.platform.pipelines.registry.RegistryRegulations
import com.epam.digital.data.platform.pipelines.registrycomponents.external.DockerRegistry
import com.epam.digital.data.platform.pipelines.registrycomponents.external.Keycloak
import com.epam.digital.data.platform.pipelines.registrycomponents.external.KeycloakClient
import com.epam.digital.data.platform.pipelines.registrycomponents.generated.DataComponent
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.Citus
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.GitServer
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.Redash
import com.epam.digital.data.platform.pipelines.stages.StageFactory
import com.epam.digital.data.platform.pipelines.tools.GitClient
import com.epam.digital.data.platform.pipelines.tools.Logger
import org.apache.commons.lang.RandomStringUtils

class BuildContext {
    Script script

    public final String YAML_RESOURCES_RELATIVE_PATH = "templates/kubernetes"
    public final String RESTORE_STAGE = "restore-from-backup"

    public Logger logger
    public IPlatform platform
    public StageFactory stageFactory
    public Codebase codebase
    public String dnsWildcard
    public String namespace

    public Registry registry
    public LinkedHashMap<String, DataComponent> dataComponents
    public LinkedHashMap<String, Boolean> bpmsConfigMapsChanged = [:]
    public RegistryRegulations registryRegulations
    public Keycloak keycloak
    public Citus citus
    public Redash redash
    public DockerRegistry dockerRegistry
    public GitServer gitServer
    public GitClient gitClient
    public KeycloakClient jenkinsDeployer
    public KeycloakClient historyExcerptor

    private File workDir

    BuildContext(Script script) {
        this.script = script
    }

    void initWorkDir(String basePath = "/tmp", String name = RandomStringUtils.random(10, true, true)) {
        logger.debug("Initializing workdir in ${basePath} with name \"${name}\"")
        workDir = new File("${basePath}/${name}")
        workDir.deleteDir()
        logger.debug("Workdir \"${name}\" has been sucessfully initialized")
    }

    String getWorkDir() {
        return workDir.getPath()
    }

    String getGeneratedProjectsDir() {
        return getWorkDir() + "/generated"
    }

    String getLogLevel() {
        return script.env["LOG_LEVEL"] ? script.env["LOG_LEVEL"] : "INFO"
    }

    String getParameterValue(String paramName, String defaultValue = null) {
        logger.debug("Getting value of ${paramName} parameter")
        String value = script.env["${paramName}"] ? script.env["${paramName}"] : defaultValue
        logger.debug("Value of ${paramName} is ${value}")
        return value
    }
}
