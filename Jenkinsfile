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
        stage('Build') {
            parallel {
                stage('Ubuntu') {
                    agent { label 'ubuntu' }
                    tools {
                        jdk 'JDK 1.8 (latest)'
                        maven 'Maven 3 (latest)'
                    }
                    environment {
                        LANG = 'en_US.UTF-8'
                    }
                    steps {
                        sh 'mvn -B -fae -t /home/jenkins/.m2/toolchains.xml -Djenkins -V clean install deploy'
                        junit '**/*-reports/*.xml'
                        archiveArtifacts artifacts: '**/*.jar', fingerprint: true
                        recordIssues sourceCodeEncoding: 'UTF-8', referenceJobName: 'log4j/master',
                            tools: [mavenConsole(), errorProne(), java(), // junitParser() // TODO: compare with junit step
                                taskScanner(highTags: 'FIXME', normalTags: 'TODO', includePattern: '**/*.java', excludePattern: '*/target/**')]
                    }
                }
                stage('Windows') {
                    agent { label 'Windows' }
                    tools {
                        jdk 'JDK 1.8 (latest)'
                        maven 'Maven 3 (latest)'
                    }
                    environment {
                        LANG = 'en_US.UTF-8'
                    }
                    steps {
                        bat '''
                        if exist %userprofile%\\.embedmongo\\ rd /s /q %userprofile%\\.embedmongo
                        mvn -B -fae -t f:\\jenkins\\.m2\\toolchains.xml -Dfile.encoding=UTF-8 -V clean install
                        '''
                        junit '**/*-reports/*.xml'
                    }
                }
            }
        }
    }
    post {
        regression {
            slackSend channel: 'logging',
                color: 'warning',
                message: ":disappear: Regression detected in ${env.BUILD_URL}"
            mail to: 'notifications@logging.apache.org',
                replyTo: 'dev@logging.apache.org',
                subject: "Regression in Jenkins build of ${env.JOB_NAME} (${env.BUILD_NUMBER})",
                body: """
There is a new regression detected in ${env.JOB_NAME}.

Build: ${env.BUILD_URL}
Logs: ${env.BUILD_URL}console
Tests: ${env.BUILD_URL}testReport/
Changes: ${env.BUILD_URL}changes
"""
        }
        fixed {
            slackSend channel: 'logging',
                color: 'good',
                message: ":beer_parrot: Build back to normal: ${env.BUILD_URL}"
            mail to: 'notifications@logging.apache.org',
                replyTo: 'dev@logging.apache.org',
                subject: "Jenkins build of ${env.JOB_NAME} (${env.BUILD_NUMBER}) back to normal",
                body: "See ${env.BUILD_URL} for more details."
        }
        failure {
            slackSend channel: 'logging',
                color: 'danger',
                message: ":doh: Build failed: ${env.BUILD_URL}"
            mail to: 'notifications@logging.apache.org',
                replyTo: 'dev@logging.apache.org',
                subject: "Build failure in Jenkins build of ${env.JOB_NAME} (${env.BUILD_NUMBER})",
                body: """
There is a build failure in ${env.JOB_NAME}.

Build: ${env.BUILD_URL}
Logs: ${env.BUILD_URL}console
Changes: ${env.BUILD_URL}changes
"""
        }
        unstable {
            slackSend channel: 'logging',
                color: 'warning',
                message: ":sadpanda: Build still unstable: ${env.BUILD_URL}"
        }
    }
}
