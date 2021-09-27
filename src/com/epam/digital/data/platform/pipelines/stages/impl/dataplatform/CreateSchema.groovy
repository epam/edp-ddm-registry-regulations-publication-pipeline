package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.ProjectType
import com.epam.digital.data.platform.pipelines.stages.Stage

@Stage(name = "create-schema", buildTool = ["any"], type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class CreateSchema {
    BuildContext context

    private final String LIQUIBASE_JAR = "/home/jenkins/liquibase/liquibase.jar"
    private final String LIQUIBASE_CLASSPATH = "/home/jenkins/liquibase/lib/postgresql-42.2.16.jar:" +
            "/home/jenkins/liquibase/lib/liquibase-ddm-ext.jar"
    private final String LIQUIBASE_PRE_DEPLOY_SCRIPT = "../../liquibase-repositories/changelog-master-pre-deploy.xml"
    private final String LIQUIBASE_MAIN_SCRIPT = "data-model/main-liquibase.xml"
    private final String LIQUIBASE_POST_DEPLOY_SCRIPT = "../../liquibase-repositories/changelog-master-post-deploy.xml"

    private final String DB_GRANTS_MASTER_SQL = "/etc/registry_db__grants_after_deploy_on_master.sql"
    private final String DB_GRANTS_REPLICA_SQL = "/etc/registry_db__grants_after_deploy_on_replica.sql"

    void run() {
        try {
            context.script.sh(script: "oc rsync data-model/data-load ${context.citus.masterPod}:/tmp")
        }
        catch (any) {
            context.logger.warn("Failed to copy data-model/data-load to ${context.citus.masterPod}")
        }

        String CITUS_MASTER_REGISTRY_DB_URL = "jdbc:postgresql://${context.citus.CITUS_MASTER_URL}:" +
                "${context.citus.CITUS_MASTER_PORT}/${context.registry.name}"
        String CITUS_MASTER_REP_REGISTRY_DB_URL = "jdbc:postgresql://${context.citus.CITUS_MASTER_REP_URL}:" +
                "${context.citus.CITUS_MASTER_REP_PORT}/${context.registry.name}"

        context.logger.info("Pre master")
        runLiquibase(changeLogFile: LIQUIBASE_PRE_DEPLOY_SCRIPT,
                url: CITUS_MASTER_REGISTRY_DB_URL,
                dbName: context.registry.name,
                contexts: "all,pub",
                regVersion: context.registry.version)

        context.logger.info("Pre replica")
        runLiquibase(changeLogFile: LIQUIBASE_PRE_DEPLOY_SCRIPT,
                url: CITUS_MASTER_REP_REGISTRY_DB_URL,
                dbName: context.registry.name,
                contexts: "all,sub",
                connDbname: context.registry.name,
                connHost: context.citus.CITUS_MASTER_URL,
                connPort: context.citus.CITUS_MASTER_PORT,
                regVersion: context.registry.version)

        context.logger.info("Main master")
        runLiquibase(changeLogFile: LIQUIBASE_MAIN_SCRIPT,
                url: CITUS_MASTER_REGISTRY_DB_URL,
                contexts: "all,pub")

        context.logger.info("Main replica")
        runLiquibase(changeLogFile: LIQUIBASE_MAIN_SCRIPT,
                url: CITUS_MASTER_REP_REGISTRY_DB_URL,
                contexts: "all,sub")

        context.logger.info("Post deploy master")
        runLiquibase(changeLogFile: LIQUIBASE_POST_DEPLOY_SCRIPT,
                url: CITUS_MASTER_REGISTRY_DB_URL,
                contexts: "all,pub",
                dbName: context.registry.name)

        context.logger.info("Post deploy replica")
        runLiquibase(changeLogFile: LIQUIBASE_POST_DEPLOY_SCRIPT,
                url: CITUS_MASTER_REP_REGISTRY_DB_URL,
                contexts: "all,sub",
                dbName: context.registry.name)

        context.logger.info("Grants after registry deployment")
        context.citus.psqlScript(context.citus.masterPod, DB_GRANTS_MASTER_SQL,
                "-v dbName=\"'${context.registry.name}'\" " +
                "-v appRoleName=\"'${context.citus.appRole}'\" " +
                "-v admRoleName=\"'${context.citus.adminRole}'\"")
        context.citus.psqlScript(context.citus.masterRepPod, DB_GRANTS_REPLICA_SQL,
                "-v dbName=\"'${context.registry.name}'\" " +
                "-v appRoleName=\"'${context.citus.appRole}'\"")
    }

    private void runLiquibase(LinkedHashMap params) {
        context.script.sh(script: "java -jar ${LIQUIBASE_JAR} " +
                "--classpath=${LIQUIBASE_CLASSPATH} " +
                "--driver=org.postgresql.Driver " +
                "--changeLogFile=${params.get("changeLogFile")} " +
                "--url=${params.get("url")} " +
                "--username=${context.citus.user} " +
                "--password=${context.citus.password} " +
                "--contexts=${params.get("contexts")} " +
                "--databaseChangeLogTableName=ddm_db_changelog " +
                "--databaseChangeLogLockTableName=ddm_db_changelog_lock " +
                "update " +
                "-Dconn.host=${params.get("connHost", "")} " +
                "-Dconn.dbname=${params.get("connDbname", "")} " +
                "-Dconn.port=${params.get("connPort", "")} " +
                "-Ddbname=${params.get("dbName", "")} " +
                "-Dreg_version=${params.get("regVersion", "")} " +
                "${context.logLevel == "DEBUG" ? "1>&2" : ""}")
    }
}
