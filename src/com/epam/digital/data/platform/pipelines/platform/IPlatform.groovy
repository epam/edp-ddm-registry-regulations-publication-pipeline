package com.epam.digital.data.platform.pipelines.platform

interface IPlatform {
    String getSecretValue(String secretName, String secretKey)

    String getJsonPathValue(String resourceApi, String resourceName, String jsonPath, String project)

    String podExec(String podName, String command, String container)

    void triggerDeploymentRollout(String deploymentName)

    boolean patchConfigMapKey(String configMap, String key, String value)

    void apply(String filePath)

    void deleteObject(String objectType, String objectName, Boolean force)

    boolean checkObjectExists(String objectType, String objectName, String project)

    boolean patch(String resource, String name, String jsonpath)

    void create(String resource, String name, String parameters)

    void annotate(String resource, String name, String key, String value, boolean overwrite)

    void scale(String resource, int replicas)

    String get(String resource, String name, String parameters)

    String getAll(String resource, String parameters)

    void addSccToUser(String user, String scc, String project)
}
