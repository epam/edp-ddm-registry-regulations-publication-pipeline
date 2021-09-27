package com.epam.digital.data.platform.pipelines.stages

import com.cloudbees.groovy.cps.NonCPS
import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import groovy.io.FileType
import groovy.json.JsonSlurperClassic

import java.lang.annotation.Annotation

class StageFactory {
    BuildContext context

    private LinkedHashMap<String, Class> stageClasses

    StageFactory(BuildContext context) {
        this.context = context
    }

    void init() {
        stageClasses = [:]
        loadStages()
    }

    def getStagesToRun() {
        context.logger.debug("Get stages from STAGES build parameter")
        String stagesConfig = context.getParameterValue("STAGES")
        if (!stagesConfig?.trim())
            context.logger.error("Parameter STAGES is mandatory to be specified, please check configuration of job")
        try {
            def stagesToRun = new JsonSlurperClassic().parseText(stagesConfig)
            context.logger.debug("Stages to run:\n${stagesToRun}")
            return stagesToRun
        }
        catch (any) {
            context.logger.error("Couldn't parse stages configuration from parameter " +
                    "STAGE - not valid JSON format.")
        }
    }

    void runStage(String stageName, BuildContext context, String runStageName = null) {
        context.script.stage(runStageName ? runStageName : stageName) {
            if (context.codebase) {
                getStageClass(stageName.toLowerCase(),
                        context.codebase.buildToolSpec,
                        context.codebase.type).run()
            }
            else {
                getStageClass(stageName.toLowerCase()).run()
            }
        }
    }

    Object getStageClass(String name, String buildTool = null, String type = null) {
        Class stageClass = stageClasses.get(stageKey(name, buildTool, type)) ?:
                stageClasses.get(stageKey(name, "any", type))

        if (!stageClass) {
            context.logger.error("There are no implementation for stage: ${name} " +
                    "build tool: ${buildTool}, type: ${type}")
        }
        return stageClass.newInstance(context: context)
    }

    @NonCPS
    private void loadStages() {
        String stagesRelativePath = "com/epam/digital/data/platform/pipelines/stages/impl"
        ArrayList<Class> classesList = []
        Enumeration<URL> res = Thread.currentThread()
                .getContextClassLoader()
                .getResources(stagesRelativePath)

        File dir = new File(res.nextElement().getFile())
        dir.eachDirRecurse() { directory ->
            directory.eachFile(FileType.FILES) { file ->
                classesList.push(Class.forName("${stagesRelativePath.replaceAll('/', '.')}." +
                        "${directory.path.replace("${dir.path}/", "").replaceAll('/', '.')}." +
                        "${file.name.replace(".groovy", "")}"))
            }
        }

        classesList.each {
            add(it)
        }
    }

    @NonCPS
    private void add(final Class clazz) {
        Annotation stageAnnotation = clazz.getAnnotation(Stage)
        if (stageAnnotation) {
            stageAnnotation.buildTool().each { tool ->
                stageAnnotation.type().each { type ->
                    stageClasses.put(stageKey(stageAnnotation.name(), tool, type.getValue().toLowerCase()), clazz)
                }
            }
        }
    }

    @NonCPS
    private static String stageKey(final String name, final String buildTool, final String type) {
        return name + buildTool + type
    }
}
