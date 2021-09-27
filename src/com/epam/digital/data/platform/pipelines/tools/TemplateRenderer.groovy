package com.epam.digital.data.platform.pipelines.tools

import com.cloudbees.groovy.cps.NonCPS
import groovy.text.GStringTemplateEngine

class TemplateRenderer {
    @NonCPS
    static String renderTemplate(String template, LinkedHashMap binding) {
        return new GStringTemplateEngine()
                .createTemplate(template)
                .make(binding)
                .toString()
    }
}
