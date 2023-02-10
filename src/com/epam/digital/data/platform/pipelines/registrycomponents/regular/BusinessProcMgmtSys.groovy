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

package com.epam.digital.data.platform.pipelines.registrycomponents.regular

class BusinessProcMgmtSys {
    public final static String URL = "http://bpms:8080"
    public final static String BPMS_DEPLOYMENT_NAME = "bpms"
    public final static String BP_ADMIN_PORTAL_DEPLOYMENT_NAME = "business-process-administration-portal"
    public final static String USER_PROCESS_MANAGEMENT_DEPLOYMENT_NAME = "user-process-management"
    public final static String GLOBAL_VARS_CONFIG_MAP = "bpms-camunda-global-system-vars"
    public final static String BP_GROUPING_CONFIG_MAP = "bp-grouping"
    public final static String DEPLOY_API_PATH = "api/deployment/create"
}
