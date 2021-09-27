package com.epam.digital.data.platform.pipelines.registry

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class Registry {

    public String name
    public String version
    public String SETTINGS_FILE = "settings.yaml"

    private final BuildContext context

    Registry(BuildContext context) {
        this.context = context
    }

    void init() {
        LinkedHashMap settingsYaml = context.script.readYaml file: SETTINGS_FILE
        this.name = settingsYaml["settings"]["general"]["register"]
        this.version = settingsYaml["settings"]["general"]["version"]
    }

    @Override
    String toString() {
        return "Registry{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                '}'
    }
}
