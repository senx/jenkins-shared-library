def call(Map pipelineParams) {

    pipeline {
        agent any

        options {
            disableConcurrentBuilds()
            buildDiscarder(logRotator(numToKeepStr: '3'))
            timestamps()
        }

        parameters {
            string(name: 'nexusHost', defaultValue: 'http://localhost:8081', description: 'Local Nexus')
            string(name: 'gpgKeyName', defaultValue: 'BD49DA0A', description: 'Gpg key to sign the artifacts')
        }

        environment {
            GPG_KEY_NAME = "${params.gpgKeyName}"
            NEXUS_HOST = "${params.nexusHost}"
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
                        version = ""
                        notify.slack('STARTED')
                    }
                    git url: pipelineParams.scmUrl
                    script {
                        version = gitUtils.getVersion()
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

            stage('Publish to:\nLocal Nexus\nMaven Central\nWarpFleet') {
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
                    echo "wf publish"
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