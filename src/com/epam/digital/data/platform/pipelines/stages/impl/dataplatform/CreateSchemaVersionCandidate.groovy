/*
 * Copyright 2023 EPAM Systems.
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

@Stage(name = "create-schema-version-candidate", buildTool = ["any"], type = [ProjectType.LIBRARY])
class CreateSchemaVersionCandidate {
    BuildContext context

    private final String LIQUIBASE_JAR = "/home/jenkins/liquibase/liquibase.jar"
    private final String LIQUIBASE_CLASSPATH = "/home/jenkins/liquibase/lib/postgresql-42.2.16.jar:" +
            "/home/jenkins/liquibase/lib/liquibase-ddm-ext.jar"
    private final String LIQUIBASE_PRE_DEPLOY_SCRIPT = "../../liquibase-repositories/changelog-master-pre-deploy.xml"
    private final String LIQUIBASE_MAIN_SCRIPT = "data-model/main-liquibase.xml"
    private final String LIQUIBASE_POST_DEPLOY_SCRIPT = "../../liquibase-repositories/changelog-master-post-deploy.xml"

    void run() {
        String registryVersionFromMaster = context.registry.version
        String dataLoadPath = "/pgdata/data-load-${context.script.env.GERRIT_CHANGE_NUMBER}"
        String symlinkPath = "/tmp/data-load-${context.script.env.GERRIT_CHANGE_NUMBER}"

        String operationalMasterRegistryDBUrl = "jdbc:postgresql://${context.postgres.OPERATIONAL_MASTER_URL}:" +
                "${context.postgres.OPERATIONAL_MASTER_PORT}/registry_dev_${context.script.env.GERRIT_CHANGE_NUMBER}"

        context.script.sshagent(["${context.gitServer.credentialsId}"]) {
            context.script.sh(script: "git fetch origin ${context.script.env.GERRIT_REFSPEC} && git checkout FETCH_HEAD")
        }

        boolean doesVersionCandidateHasChangesInDataLoad = context.script.sh(script: "if [[ -n \$(git diff HEAD HEAD~1 --stat | grep data-model) ]]; then echo true; else echo false; fi", returnStdout: true).trim().toBoolean()
        LinkedHashMap settingsYaml = context.script.readYaml file: context.registry.SETTINGS_FILE
        String registryVersionFromVersionCandidate = settingsYaml["settings"]["general"]["version"]

        boolean isDataModelChanged = isDataModelChanged(operationalMasterRegistryDBUrl, symlinkPath)

        if (isDataModelChanged) {
            // always drop temporary version candidate database if exists
            context.script.sh(script: "git checkout master")
            context.logger.info("Remove old temporary database registry_dev_${context.script.env.GERRIT_CHANGE_NUMBER}")
            context.platform.podExec(context.postgres.masterPod, "bash -c 'PGPASSWORD=\"${context.postgres.operational_pg_password}\" dropdb --force --if-exists registry_dev_${context.script.env.GERRIT_CHANGE_NUMBER} -h localhost'", "database")

            // create temporary version candidate database
            context.logger.info("Create new temporary database registry_dev_${context.script.env.GERRIT_CHANGE_NUMBER}")
            context.platform.podExec(context.postgres.masterPod, "bash -c 'PGPASSWORD=\"${context.postgres.operational_pg_password}\" psql -d registry_template -h localhost -c \"select pid, pg_terminate_backend(pid) from pg_stat_activity where datname = current_database() and pid <> pg_backend_pid();\"'", "database")
            context.platform.podExec(context.postgres.masterPod, "bash -c 'createdb -O ${context.postgres.regTemplateOwnerRole} -T registry_template registry_dev_${context.script.env.GERRIT_CHANGE_NUMBER}'", "database")

            // set searh_path to temp database
            context.platform.podExec(context.postgres.masterPod, "bash -c \"psql -c \'alter database registry_dev_${context.script.env.GERRIT_CHANGE_NUMBER} set search_path to \\\"\\\$user\\\", registry, public;\'\"", "database")

            // grant connect to registry_regulation_management_role
            context.platform.podExec(context.postgres.masterPod, "bash -c 'psql -c \"grant connect on database registry_dev_${context.script.env.GERRIT_CHANGE_NUMBER} to ${context.postgres.regRegulationRole};\"'", "database")

            // remove data-load from previous using and copy data-load files from master branch
            context.logger.info("Remove data-load from previous using")
            try {
                removeDataLoadFiles(dataLoadPath, symlinkPath)
                context.logger.info("Copying data-load from master branch")
                copyDataLoadFiles(dataLoadPath, symlinkPath)
            }
            catch (any) {
                context.logger.warn("Failed to copy data-model/data-load to ${context.postgres.masterPod}")
            }

            context.logger.info("Applying Liquibase from master")
            runUpdateLiquibase(operationalMasterRegistryDBUrl ,registryVersionFromMaster, dataLoadPath, symlinkPath)

            if (doesVersionCandidateHasChangesInDataLoad) {
                context.logger.info("Cloning ${context.script.env.GERRIT_CHANGE_NUMBER} version candidate")
                context.script.sshagent(["${context.gitServer.credentialsId}"]) {
                    context.script.sh(script: "git fetch origin ${context.script.env.GERRIT_REFSPEC} && git checkout FETCH_HEAD")
                }

                // remove data from previous using and copy data-load files from version candidate
                context.logger.info("Remove data-load from previous using")
                try {
                    removeDataLoadFiles(dataLoadPath, symlinkPath)
                    context.logger.info("Copying data-load from version candidate")
                    copyDataLoadFiles(dataLoadPath, symlinkPath)
                }
                catch (any) {
                    context.logger.warn("Failed to copy data-model/data-load to ${context.postgres.masterPod}")
                }

                context.logger.info("Applying Liquibase from ${context.script.env.GERRIT_CHANGE_NUMBER} version candidate")
                runUpdateLiquibase(operationalMasterRegistryDBUrl, registryVersionFromVersionCandidate, dataLoadPath, symlinkPath)
            }
            else {
                context.logger.info("Skip applying Liquibase from version candidate.")
            }
        }
        else
            context.logger.info("Data-Model hasn't been changed. Skip applying liquibase.")
    }

    private String runLiquibase(LinkedHashMap params, String method) {
        String statusOfLiquibase = context.script.sh(script: "set +x; java -jar ${LIQUIBASE_JAR} " +
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
                "${method} " +
                "-Dconn.host=${params.get("connHost", "")} " +
                "-Dconn.dbname=${params.get("connDbname", "")} " +
                "-Dconn.port=${params.get("connPort", "")} " +
                "-Ddbname=${params.get("dbName", "")} " +
                "-Dreg_version=${params.get("regVersion", "")} " +
                "-DdataLoadPath=${params.get("dataLoadPath", "")} " +
                "${context.logLevel == "DEBUG" ? "1>&2" : ""}", returnStdout: true)
        context.script.println(statusOfLiquibase)
        return statusOfLiquibase
    }

    private void runUpdateLiquibase(String operationalMasterRegistryDBUrl, String registryVersion, String dataLoadPath, String symlinkPath) {
        context.logger.info("Run pre-deploy script on operational-instance")
        runLiquibase(changeLogFile: LIQUIBASE_PRE_DEPLOY_SCRIPT,
                url: operationalMasterRegistryDBUrl,
                dbName: "registry_dev_${context.script.env.GERRIT_CHANGE_NUMBER}",
                contexts: "all,pub",
                regVersion: registryVersion,
                dataLoadPath: symlinkPath + "/",
                username: context.postgres.regTemplateOwnerRole,
                password: context.postgres.regTemplateOwnerRolePass,
                "update")

        context.logger.info("Run main-deploy script on operational-instance")
        runLiquibase(changeLogFile: LIQUIBASE_MAIN_SCRIPT,
                url: operationalMasterRegistryDBUrl,
                contexts: "all,pub",
                dataLoadPath: symlinkPath + "/",
                username: context.postgres.regTemplateOwnerRole,
                password: context.postgres.regTemplateOwnerRolePass,
                "update")

        context.logger.info("Run post-deploy script on operational-instance")
        runLiquibase(changeLogFile: LIQUIBASE_POST_DEPLOY_SCRIPT,
                url: operationalMasterRegistryDBUrl,
                contexts: "all,pub",
                dbName: "registry_dev_${context.script.env.GERRIT_CHANGE_NUMBER}",
                regVersion: registryVersion,
                dataLoadPath: symlinkPath + "/",
                username: context.postgres.regTemplateOwnerRole,
                password: context.postgres.regTemplateOwnerRolePass,
                "update")

        removeDataLoadFiles(dataLoadPath, symlinkPath)
    }

    private boolean isDataModelChanged(String operationalMasterRegistryDBUrl, String symlinkPath) {
        context.logger.info("Check if data-model has been changed")
        String liquibaseStatus
        try {
            liquibaseStatus = runLiquibase(changeLogFile: LIQUIBASE_MAIN_SCRIPT,
                    url: operationalMasterRegistryDBUrl,
                    contexts: "all,pub",
                    dataLoadPath: symlinkPath + "/",
                    username: context.postgres.regTemplateOwnerRole,
                    password: context.postgres.regTemplateOwnerRolePass,
                    "status")
        } catch(any) {
            context.logger.info("Failed to run liquibase or database not found")
            liquibaseStatus = "Failed to run liquibase or database not found"
        }
        return liquibaseStatus.contains("is up to date") ? false : true
    }

    private void removeDataLoadFiles(String dataLoadPath, String symlinkPath) {
        if (context.platform.podExec(context.postgres.masterPod, "bash -c 'if [[ -d \"${dataLoadPath}\" ]] || [[ -L \"${symlinkPath}\" ]]; then echo true; fi'", "database").toBoolean()) {
            context.platform.podExec(context.postgres.masterPod, "bash -c 'rm -rf $dataLoadPath || :'", "database")
            context.platform.podExec(context.postgres.masterPod, "bash -c 'rm $symlinkPath || :'", "database")
        } else {
            context.logger.info("There are no data-load files to remove")
        }
    }

    private void copyDataLoadFiles(String dataLoadPath, String symlinkPath) {
        context.platform.podExec(context.postgres.masterPod, "bash -c 'mkdir ${dataLoadPath}'", "database")
        context.platform.podExec(context.postgres.masterPod, "bash -c 'ln -s ${dataLoadPath}/data-load/ $symlinkPath'", "database")
        context.script.sh(script: "oc rsync data-model/data-load ${context.postgres.masterPod}:${dataLoadPath}")
    }
}
