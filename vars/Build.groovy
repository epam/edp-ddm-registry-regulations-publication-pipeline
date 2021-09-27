import com.epam.digital.data.platform.pipelines.buildcontext.BuildContext
import com.epam.digital.data.platform.pipelines.codebase.Codebase
import com.epam.digital.data.platform.pipelines.platform.PlatformFactory
import com.epam.digital.data.platform.pipelines.registrycomponents.external.DockerRegistry
import com.epam.digital.data.platform.pipelines.registrycomponents.regular.Gerrit
import com.epam.digital.data.platform.pipelines.stages.StageFactory
import com.epam.digital.data.platform.pipelines.tools.GitClient
import com.epam.digital.data.platform.pipelines.tools.Logger

void call() {
    BuildContext context

    node("master") {
        stage("Init") {
            context = new BuildContext(this)

            String logLevel = context.getLogLevel()
            context.logger = new Logger(context.script)
            context.logger.init(logLevel)
            context.logger.info("Sucessfully inialized logger with level ${logLevel}")

            context.namespace = context.getParameterValue("CI_NAMESPACE")
            context.logger.debug("Current namespace: ${context.namespace}")

            context.platform = new PlatformFactory(context).getPlatformImpl()
            context.logger.debug("Current platform: ${context.platform.class.name}")

            context.logger.info("Initializing docker registry")
            context.dockerRegistry = new DockerRegistry(context)
            context.dockerRegistry.init()
            context.logger.debug(context.dockerRegistry.toString())

            context.codebase = new Codebase(context)
            context.codebase.init()
            context.logger.debug("Codebase config: ${context.codebase.toString()}")

            context.stageFactory = new StageFactory(context)
            context.stageFactory.init()

            context.dnsWildcard = context.platform.getJsonPathValue("jenkins", "jenkins", ".spec.edpSpec.dnsWildcard")
            context.logger.debug("Current dns wildcard: ${context.dnsWildcard}")

            context.gitServer = new Gerrit(context, "gerrit")
            context.gitServer.init()
            context.logger.debug("Gitserver config: ${context.gitServer.toString()}")

            context.gitClient = new GitClient(context)
        }
    }

    node(context.codebase.jenkinsAgent) {
        context.initWorkDir()
        context.codebase.initBuildTool()

        context.stageFactory.getStagesToRun().each { stage ->
            dir(context.getWorkDir()) {
                dir(context.workDir) {
                    context.stageFactory.runStage(stage.name, context)
                }
            }
        }
    }
}
