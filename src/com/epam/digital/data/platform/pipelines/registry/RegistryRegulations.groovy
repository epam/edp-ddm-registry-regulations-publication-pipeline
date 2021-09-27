package com.epam.digital.data.platform.pipelines.registry

import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext

class RegistryRegulations {
    private final BuildContext context

    public LinkedHashMap<RegulationType, ArrayList<String>> filesToDeploy = [:]

    RegistryRegulations(BuildContext context) {
        this.context = context
    }

    ArrayList<String> getChangedRegulations(RegulationType regulationType) {
        ArrayList<String> changedRegulations
        boolean fullDeploy = context.getParameterValue("FULL_DEPLOY", "false").toBoolean()
        if (fullDeploy) {
            changedRegulations = getAllRegulations(regulationType)
        } else {
            context.logger.info("Get changed ${regulationType.value} files")
            try {
                changedRegulations = context.script.sh(script: "git diff HEAD~1 HEAD -m -1 --name-only " +
                        "--diff-filter=ACMRT " +
                        "--pretty='format:' | grep -E \"${regulationType.value}\" | grep -v .git", returnStdout: true)
                        .tokenize('\n')
                context.logger.debug(changedRegulations.toString())
            } catch (any) {
                changedRegulations = []
                context.logger.info("No changed ${regulationType.value} files found")
            }
        }
        return changedRegulations
    }

    ArrayList<String> getAllRegulations(RegulationType regulationType) {
        context.logger.info("Get all ${regulationType.value} files")
        ArrayList<String> allRegulations
        try {
            allRegulations = context.script.sh(script: "find ${regulationType.value} " +
                    "-not -path '*/\\.*' " +
                    "-type f ! -name '*.gitkeep*' | grep '.'",
                    returnStdout: true).tokenize('\n')
            context.logger.debug(allRegulations.toString())
            return allRegulations
        } catch (any) {
            context.logger.info("No ${regulationType.value} files found")
            allRegulations = []
        }
        return allRegulations
    }
}
