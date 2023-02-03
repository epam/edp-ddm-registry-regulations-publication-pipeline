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

@Stage(name = "cleanup-of-version-candidate-dbs", buildTool = ["any"], type = [ProjectType.LIBRARY])
class cronCleanupOfVersionCandidateDatabases {
    BuildContext context

    void run() {
        ArrayList<String> versionCandidateIdArray = new ArrayList<String>()
        ArrayList<String> crunchyDBsArray = new ArrayList<String>()
        ArrayList<String> versionCandidateTemporaryDBsArray = new ArrayList<String>()

        context.logger.info("Running cleanup for the version candidate databases")
        context.script.sshagent(["${context.gitServer.credentialsId}"]) {
            versionCandidateIdArray = context.script.sh(script: "ssh -oStrictHostKeyChecking=no -p ${context.gitServer.sshPort} " +
                    "${context.gitServer.autouser}@${context.gitServer.host} gerrit query --format=JSON status:open project:registry-regulations " +
                    "| jq '.number | select(. != null)'", returnStdout: true).tokenize('\n')
            crunchyDBsArray = context.postgres.psqlCommand(context.postgres.masterPod, "select datname from pg_database",
                    "postgres", context.postgres.operational_pg_user).tokenize('\n')
            crunchyDBsArray.each {
                it.contains("registry_dev") ? versionCandidateTemporaryDBsArray.add(it.split("registry_dev_")[1]) : false
            }
        }
        versionCandidateTemporaryDBsArray.each {
            if (versionCandidateIdArray.contains(it))
                true
            else {
                context.logger.info("Removing temporary version candidate DBs")
                context.postgres.psqlCommand(context.postgres.masterPod,
                        "drop database if exists registry_dev_${it} with(force)",
                        "postgres", context.postgres.operational_pg_user)
            }
        }
    }
}
