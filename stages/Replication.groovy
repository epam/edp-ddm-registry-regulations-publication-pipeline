import com.epam.edp.stages.impl.ci.ProjectType
import com.epam.edp.stages.impl.ci.Stage

@Stage(name = "replication", buildTool = "any", type = [ProjectType.APPLICATION, ProjectType.LIBRARY])
class Replication {
    Script script

    void run(context) {
        script.dir("${context.workDir}") {
            script.withCredentials([script.sshUserPrivateKey(credentialsId: "gerrit-ciuser-sshkey",
                    keyFileVariable: 'key', passphraseVariable: '', usernameVariable: 'git_user')]) {
                def gerrit = [:]
                gerrit.user = "jenkins"
                gerrit.host = "gerrit"
                gerrit.port = "32114"

                script.sh """
                    mkdir -p ~/.ssh
                    eval `ssh-agent`
                    ssh-add ${script.key}
                    ssh-keyscan -p ${gerrit.port} ${gerrit.host} >> ~/.ssh/known_hosts
                    git config --global user.email ${gerrit.user}@epam.com
                    git config --global user.name ${gerrit.user}
                    git remote set-url origin ssh://${gerrit.user}@${gerrit.host}:${gerrit.port}/${context.codebase.name}
                    git checkout -b ${context.git.branch}
                    git push origin --all --force
                """
            }
        }
    }
}

return Replication