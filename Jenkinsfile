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
                    steps {
                        sh 'mvn -B -fn -t toolchains-jenkins-ubuntu.xml -Djenkins -V clean install'
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: '**/*.jar', fingerprint: true
                            junit '**/*-reports/*.xml'
                            recordIssues enabledForFailure: true,
                                tool: mavenConsole(),
                                referenceJobName: 'log4j/release-2.x'
                            recordIssues enabledForFailure: true,
                                tool: errorProne(),
                                referenceJobName: 'log4j/release-2.x'
                            recordIssues enabledForFailure: true,
                                tool: java(),
                                sourceCodeEncoding: 'UTF-8',
                                referenceJobName: 'log4j/release-2.x'
                            recordIssues enabledForFailure: true,
                                tool: taskScanner(includePattern: '**/*.java', excludePattern: 'target/**', highTags: 'FIXME', normalTags: 'TODO'),
                                sourceCodeEncoding: 'UTF-8',
                                referenceJobName: 'log4j/release-2.x'
                        }
                    }
                }
                stage('Windows') {
                    agent { label 'Windows' }
                    tools {
                        jdk 'JDK 1.8 (latest)'
                        maven 'Maven 3 (latest)'
                    }
                    steps {
                        bat '''
                        if exist %userprofile%\\.embedmongo\\ rd /s /q %userprofile%\\.embedmongo
                        mvn -B -fn -t toolchains-jenkins-win.xml -Dfile.encoding=UTF-8 -V clean install
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
        regression {
            slackSend channel: 'logging', message: "Regression detected in ${env.BUILD_URL}", color: 'danger'
        }
        fixed {
            slackSend channel: 'logging', message: "Build back to normal: ${env.BUILD_URL}", color: 'good'
        }
    }
}
