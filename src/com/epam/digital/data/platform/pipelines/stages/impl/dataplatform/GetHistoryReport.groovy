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

package com.epam.digital.data.platform.pipelines.stages.impl.dataplatform

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.stages.Stage
import com.epam.digital.data.platform.pipelines.stages.ProjectType

@Stage(name = "get-history-report", buildTool = "any", type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class GetHistoryReport {
    BuildContext context

    void run() {
        String JOB_NAME = "receive-data-job-${context.script.env.BUILD_NUMBER}"
        String REPORT_PATH = "excerpt.pdf"
        context.platform.waitFor("job/${JOB_NAME}", "condition=complete", "250s")
        context.script.httpRequest acceptType: 'APPLICATION_OCTETSTREAM',
                url: context.platform.getJsonPathValue("jobs", JOB_NAME, ".metadata.annotations.link"),
                httpMode: 'GET',
                outputFile: REPORT_PATH,
                customHeaders: [[name : 'X-Access-Token', maskValue: true,
                                 value: context.keycloak.getAccessToken(context.historyExcerptor)]]
        context.script.archiveArtifacts artifacts: REPORT_PATH
        context.platform.deleteObject("job", JOB_NAME)
    }
}
