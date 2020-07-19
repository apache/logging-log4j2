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
    triggers {
        // TODO: this can be removed once gitbox webhooks are re-enabled
        pollSCM 'H/5 * * * *'
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
                        // https://issues.jenkins-ci.org/browse/JENKINS-43353
                        script {
                            def buildNumber = BUILD_NUMBER as int
                            if (buildNumber > 1) milestone(buildNumber - 1)
                            milestone(buildNumber)
                        }
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
                                    referenceJobName: 'log4j/master',
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
            emailext to: 'notifications@logging.apache.org',
                from: 'Mr. Jenkins <jenkins@ci-builds.apache.org>',
                subject: "[CI][SUCCESS] ${env.JOB_NAME}#${env.BUILD_NUMBER} back to normal",
                body: '${SCRIPT, template="groovy-text.template"}'
        }
        failure {
            emailext to: 'notifications@logging.apache.org',
                from: 'Mr. Jenkins <jenkins@ci-builds.apache.org>',
                subject: "[CI][FAILURE] ${env.JOB_NAME}#${env.BUILD_NUMBER} has potential issues",
                body: '${SCRIPT, template="groovy-text.template"}'
        }
    }
}
