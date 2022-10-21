def call(Map pipelineParams) {

    pipeline {
        agent any

        options {
            disableConcurrentBuilds()
            buildDiscarder(logRotator(numToKeepStr: '3'))
            timestamps()
        }

        environment {
            GPG_KEY_NAME = "${env.gpgKeyName}"
            NEXUS_HOST = "${env.nexusHost}"
            NEXUS_CREDS = credentials('nexus')
            OSSRH_CREDS = credentials('ossrh')
            GRADLE_CMD = './gradlew \
                -Psigning.gnupg.keyName=$GPG_KEY_NAME \
                -PossrhUsername=$OSSRH_CREDS_USR \
                -PossrhPassword=$OSSRH_CREDS_PSW \
                -PnexusHost=$NEXUS_HOST \
                -PnexusUsername=$NEXUS_CREDS_USR \
                -PnexusPassword=$NEXUS_CREDS_PSW'
        }

        stages {
            stage('Checkout') {
                steps {
                    script {
                        env.version = ""
                        notify.slack('STARTED')
                    }
                    git url: pipelineParams.scmUrl
                    script {
                        env.version = gitUtils.getVersion()
                    }
                }
            }

            stage('Build') {
                steps {
                    sh "$GRADLE_CMD clean build -x test"
                }
            }

            stage('Test') {
                steps {
                    sh "$GRADLE_CMD test"
                }
            }

            stage('Publish to: Local Nexus, Maven Central and, WarpFleet') {
                when {
                    beforeInput true
                    expression { gitUtils.isTag() }
                }
                options {
                    timeout(time: 4, unit: 'DAYS')
                }
                input {
                    message "Should we deploy module to:\n'Local Nexus',\n'Maven Central',\n and WarpFleet?"
                }
                steps {
                    sh "$GRADLE_CMD publish closeAndReleaseStagingRepository"
                    sh "sleep 900" // Wait 15 minutes for the components to be available on Maven Central
                    sh "~/.nvm/current/bin/wf publish ${env.version} ${pipelineParams.scmUrl}"
                    sh "~/.nvm/current/bin/wf publish ${env.version}-uberjar ${pipelineParams.scmUrl}"
                }
            }
        }
        post {
            success {
                script {
                    notify.slack('SUCCESSFUL')
                }
            }
            failure {
                script {
                    notify.slack('FAILURE')
                }
            }
            aborted {
                script {
                    notify.slack('ABORTED')
                }
            }
            unstable {
                script {
                    notify.slack('UNSTABLE')
                }
            }
        }
    }
}