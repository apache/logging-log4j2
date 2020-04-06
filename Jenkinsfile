#!groovy
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

pipeline {
    options {
        ansiColor('xterm')
        buildDiscarder logRotator(numToKeepStr: '10')
        timeout time: 2, unit: 'HOURS'
        parallelsAlwaysFailFast()
        durabilityHint 'PERFORMANCE_OPTIMIZED'
    }
    agent none
    stages {
        stage('Checkout') {
        parallel {
            stage('Ubuntu') {
                agent { label 'ubuntu' }
                tools {
                    jdk 'JDK 1.8 (latest)'
                    maven 'Maven 3 (latest)'
                }
                environment {
                    LANG = 'C.UTF-8'
                }
                steps {
                    sh 'mvn -B -fae -t toolchains-jenkins-ubuntu.xml -Djenkins -V clean install deploy'
                }
                post {
                    success {
                        archiveArtifacts artifacts: '**/*.jar', fingerprint: true
                    }
                    always {
                        junit '**/*-reports/*.xml'
                        recordIssues enabledForFailure: true,
                            sourceCodeEncoding: 'UTF-8',
                            referenceJobName: 'log4j/master',
                            tools: [mavenConsole(), errorProne(), java(),
                                taskScanner(highTags: 'FIXME', normalTags: 'TODO', includePattern: '**/*.java', excludePattern: '*/target/**')]
                    }
                }
            }
            stage('Windows') {
                agent { label 'Windows' }
                tools {
                    jdk 'JDK 1.8 (latest)'
                    maven 'Maven 3 (latest)'
                }
                environment {
                    LANG = 'C.UTF-8'
                }
                steps {
                    bat '''
                    if exist %userprofile%\\.embedmongo\\ rd /s /q %userprofile%\\.embedmongo
                    mvn -B -fae -t toolchains-jenkins-win.xml -Dproject.build.sourceEncoding=UTF-8 -V clean install
                    '''
                }
                post {
                    always {
                        junit '**/*-reports/*.xml'
                    }
                }
            }
        }
    }
    }
    post {
        fixed {
            slackSend channel: 'logging',
                color: 'good',
                message: ":excellent: <${env.JOB_URL}|${env.JOB_NAME}> was fixed in <${env.BUILD_URL}|build #${env.BUILD_NUMBER}>."
            mail to: 'notifications@logging.apache.org',
                from: 'Mr. Jenkins <jenkins@builds.apache.org>',
                subject: "Jenkins job ${env.JOB_NAME}#${env.BUILD_NUMBER} back to normal",
                body: """
The build for ${env.JOB_NAME} completed successfully and is back to normal.

Build: ${env.BUILD_URL}
Logs: ${env.BUILD_URL}console
Changes: ${env.BUILD_URL}changes

--
Mr. Jenkins
Director of Continuous Integration
"""
        }
        failure {
            slackSend channel: 'logging',
                color: 'danger',
                message: ":doh: <${env.JOB_URL}|${env.JOB_NAME}> failed in <${env.BUILD_URL}|build #${env.BUILD_NUMBER}>."
            mail to: 'notifications@logging.apache.org',
                from: 'Mr. Jenkins <jenkins@builds.apache.org>',
                subject: "Jenkins job ${env.JOB_NAME}#${env.BUILD_NUMBER} failed",
                body: """
There is a build failure in ${env.JOB_NAME}.

Build: ${env.BUILD_URL}
Logs: ${env.BUILD_URL}console
Changes: ${env.BUILD_URL}changes

--
Mr. Jenkins
Director of Continuous Integration
"""
        }
        unstable {
            slackSend channel: 'logging',
                color: 'warning',
                message: ":disappear: <${env.JOB_URL}|${env.JOB_NAME}> is unstable in <${env.BUILD_URL}|build #${env.BUILD_NUMBER}>."
        }
    }
}
