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

class FormManagement {
    public final static String AUTH_DATABASE = "admin"

    public final static String PROVIDER_DEPLOYMENT_NAME = "form-management-provider"
    public final static String PROVIDER_DB_NAME = "formio"
    public final static String PROVIDER_DB_SECRET = "form-management-provider-db-credentials"
    public final static String PROVIDER_DB_POD = "form-management-provider-db-0"
    public final static String PROVIDER_DB_CONTAINER = "form-management-provider-db"
    public final static String PROVIDER_URL = "http://form-management-provider:3001"

    public final static String MODELER_DEPLOYMENT_NAME = "form-management-modeler"
    public final static String MODELER_DB_NAME = "formio"
    public final static String MODELER_DB_SECRET = "form-management-modeler-db-credentials"
    public final static String MODELER_DB_POD = "form-management-modeler-db-0"
    public final static String MODELER_DB_CONTAINER = "form-management-modeler-db"
}
