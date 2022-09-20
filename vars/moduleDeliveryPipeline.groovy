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
                    }
                    // notify.build('STARTED')
                    git url: pipelineParams.scmUrl
                }
            }

            stage('Debug') {
                steps{
                    sh "set"
                    echo "============================================================"
                    sh "env"
                    echo "============================================================"
                    echo "Building $env.BRANCH_NAME"
                    echo "Building $env.TAG_NAME"
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
                    buildingTag()
                }
                options {
                    timeout(time: 4, unit: 'DAYS')
                }
                input {
                    message "Should we deploy module to\n'Maven Central',\n'Local Nexus',\n and WarpFleet?"
                }
                steps {
                    sh "$GRADLE_CMD publish closeAndReleaseStagingRepository"
                    echo "wf publish"
                    // notify.build('SUCCESSFUL')
                }
            }
        }
        // post {
        //     success {
        //         notify.build('SUCCESSFUL')
        //     }
        //     failure {
        //         notify.build('FAILURE')
        //     }
        //     aborted {
        //         notify.build('ABORTED')
        //     }
        //     unstable {
        //         notify.build('UNSTABLE')
        //     }
        // }
    }
}