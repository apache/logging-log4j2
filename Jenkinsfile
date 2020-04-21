#!groovy
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

// =================================================================
// https://cwiki.apache.org/confluence/display/LOGGING/Jenkins+Setup
// =================================================================

// general pipeline documentation: https://jenkins.io/doc/book/pipeline/syntax/
pipeline {
    // https://jenkins.io/doc/book/pipeline/syntax/#options
    options {
        // support ANSI colors in stdout/stderr
        ansiColor 'xterm'
        // only keep the latest 10 builds
        buildDiscarder logRotator(numToKeepStr: '10')
        // cancel build if not complete within two hours of scheduling
        timeout time: 2, unit: 'HOURS'
        // fail parallel stages as soon as any of them fail
        parallelsAlwaysFailFast()
    }
    // https://jenkins.io/doc/book/pipeline/syntax/#agent
    // start with no Jenkins agent allocated as they will only be needed for the individual stages
    // therefore, anything in the top level post section can only contain steps that don't require a Jenkins agent
    // (such as slackSend, mail, etc.)
    agent none
    stages {
        stage('Continuous Integration') {
            // https://jenkins.io/doc/book/pipeline/syntax/#parallel
            parallel {
                stage('Ubuntu') {
                    agent {
                        // https://cwiki.apache.org/confluence/display/INFRA/Jenkins+node+labels
                        label 'ubuntu'
                    }
                    // https://jenkins.io/doc/book/pipeline/syntax/#tools
                    tools {
                        // https://cwiki.apache.org/confluence/display/INFRA/JDK+Installation+Matrix
                        jdk 'JDK 1.8 (latest)'
                        // https://cwiki.apache.org/confluence/display/INFRA/Maven+Installation+Matrix
                        maven 'Maven 3 (latest)'
                    }
                    // https://jenkins.io/doc/book/pipeline/syntax/#environment
                    environment {
                        LANG = 'C.UTF-8'
                    }
                    steps {
                        // build, test, and deploy snapshots
                        // note that the jenkins system property is set here to activate certain pom properties in
                        // some log4j modules that compile against system jars (e.g., log4j-jmx-gui)
                        // also note that the Jenkins agents on builds.a.o already have an ~/.m2/settings.xml for snapshots
                        sh 'mvn --show-version --fail-at-end --toolchains toolchains-jenkins-ubuntu.xml -Djenkins clean install deploy'
                    }
                    post {
                        always {
                            // record linux run of tests
                            junit '**/*-reports/*.xml'
                            // additional warnings generated during build
                            // TODO: would be nice to be able to include checkstyle, cpd, pmd, and spotbugs,
                            //       but current site build takes too long
                            recordIssues enabledForFailure: true,
                                    sourceCodeEncoding: 'UTF-8',
                                    referenceJobName: 'log4j/release-2.x',
                                    tools: [mavenConsole(), errorProne(), java(),
                                            taskScanner(highTags: 'FIXME', normalTags: 'TODO', includePattern: '**/*.java', excludePattern: '*/target/**')]
                        }
                    }
                }
                stage('Windows') {
                    agent {
                        // https://cwiki.apache.org/confluence/display/INFRA/Jenkins+node+labels
                        label 'Windows'
                    }
                    tools {
                        // https://cwiki.apache.org/confluence/display/INFRA/JDK+Installation+Matrix
                        jdk 'JDK 1.8 (latest)'
                        // https://cwiki.apache.org/confluence/display/INFRA/Maven+Installation+Matrix
                        maven 'Maven 3 (latest)'
                    }
                    environment {
                        LANG = 'C.UTF-8'
                    }
                    steps {
                        // note that previous test runs of log4j-mongodb* may have left behind an embedded mongo folder
                        // also note that we don't need to use the jenkins system property here as it's ubuntu-specific
                        bat '''
                    if exist %userprofile%\\.embedmongo\\ rd /s /q %userprofile%\\.embedmongo
                    mvn --show-version --fail-at-end --toolchains toolchains-jenkins-win.xml clean install
                    '''
                    }
                    post {
                        always {
                            // record windows run of tests
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
                    subject: "[CI][SUCCESS] ${env.JOB_NAME}#${env.BUILD_NUMBER} back to normal",
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
                    message: ":doh: <${env.JOB_URL}|${env.JOB_NAME}> failed in <${env.BUILD_URL}|build #${env.BUILD_NUMBER}>. <${env.BUILD_URL}testReport/|Tests>."
            mail to: 'notifications@logging.apache.org',
                    from: 'Mr. Jenkins <jenkins@builds.apache.org>',
                    subject: "[CI][FAILURE] ${env.JOB_NAME}#${env.BUILD_NUMBER} has potential issues",
                    body: """
There is a build failure in ${env.JOB_NAME}.

Build: ${env.BUILD_URL}
Logs: ${env.BUILD_URL}console
Test results: ${env.BUILD_URL}testReport/
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
