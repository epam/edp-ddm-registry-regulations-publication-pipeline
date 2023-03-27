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

package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "create-schema", buildTool = ["any"], type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class CreateSchema {
    BuildContext context

    private final String LIQUIBASE_JAR = "/home/jenkins/liquibase/liquibase.jar"
    private final String LIQUIBASE_CLASSPATH = "/home/jenkins/liquibase/lib/postgresql-42.3.3.jar:" +
            "/home/jenkins/liquibase/lib/liquibase-ddm-ext.jar"
    private final String LIQUIBASE_PRE_DEPLOY_SCRIPT = "../../liquibase-repositories/changelog-master-pre-deploy.xml"
    private final String LIQUIBASE_MAIN_SCRIPT = "data-model/main-liquibase.xml"
    private final String LIQUIBASE_POST_DEPLOY_SCRIPT = "../../liquibase-repositories/changelog-master-post-deploy.xml"

    void run() {
        try {
            removeDataLoadFiles()
            context.platform.podExec(context.postgres.masterPod, "bash -c 'mkdir /pgdata/data-load'", "database")
            context.platform.podExec(context.postgres.masterPod, "bash -c 'ln -s /pgdata/data-load/ /tmp/data-load'", "database")
            context.script.sh(script: "oc rsync data-model/data-load ${context.postgres.masterPod}:/pgdata")

        }
        catch (any) {
            context.logger.warn("Failed to copy data-model/data-load to ${context.postgres.masterPod}")
        }

        String OPERATIONAL_MASTER_REGISTRY_DB_URL = "jdbc:postgresql://${context.postgres.OPERATIONAL_MASTER_URL}:" +
                "${context.postgres.OPERATIONAL_MASTER_PORT}/${context.registry.name}"
        String ANALYTICAL_MASTER_REGISTRY_DB_URL = "jdbc:postgresql://${context.postgres.ANALYTICAL_MASTER_URL}:" +
                "${context.postgres.ANALYTICAL_MASTER_PORT}/${context.registry.name}"

        context.logger.info("Pre master")
        runLiquibase(changeLogFile: LIQUIBASE_PRE_DEPLOY_SCRIPT,
                url: OPERATIONAL_MASTER_REGISTRY_DB_URL,
                dbName: context.registry.name,
                contexts: "all,pub",
                regVersion: context.registry.version,
                username: context.postgres.ownerRole,
                password: context.postgres.ownerRolePass)

        context.logger.info("Pre replica")
        runLiquibase(changeLogFile: LIQUIBASE_PRE_DEPLOY_SCRIPT,
                url: ANALYTICAL_MASTER_REGISTRY_DB_URL,
                dbName: context.registry.name,
                contexts: "all,sub",
                connDbname: context.registry.name,
                connHost: context.postgres.OPERATIONAL_MASTER_URL,
                connPort: context.postgres.OPERATIONAL_MASTER_PORT,
                regVersion: context.registry.version,
                username: context.postgres.ownerRole,
                password: context.postgres.ownerRolePass)

        context.logger.info("Main master")
        runLiquibase(changeLogFile: LIQUIBASE_MAIN_SCRIPT,
                url: OPERATIONAL_MASTER_REGISTRY_DB_URL,
                contexts: "all,pub",
                username: context.postgres.ownerRole,
                password: context.postgres.ownerRolePass)

        context.logger.info("Main replica")
        runLiquibase(changeLogFile: LIQUIBASE_MAIN_SCRIPT,
                url: ANALYTICAL_MASTER_REGISTRY_DB_URL,
                contexts: "all,sub",
                username: context.postgres.ownerRole,
                password: context.postgres.ownerRolePass)

        context.logger.info("Post deploy master")
        runLiquibase(changeLogFile: LIQUIBASE_POST_DEPLOY_SCRIPT,
                url: OPERATIONAL_MASTER_REGISTRY_DB_URL,
                contexts: "all,pub",
                dbName: context.registry.name,
                regVersion: context.registry.version,
                username: context.postgres.operational_pg_user,
                password: context.postgres.operational_pg_password)

        context.logger.info("Post deploy replica")
        runLiquibase(changeLogFile: LIQUIBASE_POST_DEPLOY_SCRIPT,
                url: ANALYTICAL_MASTER_REGISTRY_DB_URL,
                contexts: "all,sub",
                dbName: context.registry.name,
                regVersion: context.registry.version,
                username: context.postgres.analytical_pg_user,
                password: context.postgres.analytical_pg_password)

        removeDataLoadFiles()
    }

    private void runLiquibase(LinkedHashMap params) {
        context.script.sh(script: "set +x; java -jar ${LIQUIBASE_JAR} " +
                "--liquibaseSchemaName=public " +
                "--classpath=${LIQUIBASE_CLASSPATH} " +
                "--driver=org.postgresql.Driver " +
                "--changeLogFile=${params.get("changeLogFile")} " +
                "--url=${params.get("url")} " +
                "--username=${params.get("username")} " +
                "--password='${params.get("password")}' " +
                "--contexts=${params.get("contexts")} " +
                "--databaseChangeLogTableName=ddm_db_changelog " +
                "--databaseChangeLogLockTableName=ddm_db_changelog_lock " +
                "--logLevel=INFO " +
                "update " +
                "-Dconn.host=${params.get("connHost", "")} " +
                "-Dconn.dbname=${params.get("connDbname", "")} " +
                "-Dconn.port=${params.get("connPort", "")} " +
                "-Ddbname=${params.get("dbName", "")} " +
                "-Dreg_version=${params.get("regVersion", "")} " +
                "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
    }

    private void removeDataLoadFiles() {
        String dataLoadPath = "/pgdata/data-load"
        String symlinkPath = "/tmp/data-load"
        if (context.platform.podExec(context.postgres.masterPod, "bash -c 'if [[ -d \"/pgdata/data-load\" ]] || [[ -L \"/tmp/data-load\" ]]; then echo true; fi'", "database").toBoolean()) {
            context.platform.podExec(context.postgres.masterPod, "bash -c 'rm -rf $dataLoadPath || :'", "database")
            context.platform.podExec(context.postgres.masterPod, "bash -c 'rm $symlinkPath || :'", "database")
        } else {
            context.logger.info("There are no data-load files to remove")
        }
    }

}
