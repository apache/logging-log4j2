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
        timeout time: 60, unit: 'MINUTES'
    }
    agent none
    stages {
        stage('Build') {
            failFast true
            parallel {
                stage('Ubuntu') {
                    agent { label 'ubuntu&&!H20' }
                    tools {
                        // https://cwiki.apache.org/confluence/display/INFRA/JDK+Installation+Matrix
                        jdk 'JDK 1.8 (latest)'
                        // https://cwiki.apache.org/confluence/display/INFRA/Maven+Installation+Matrix
                        maven 'Maven 3 (latest)'
                    }
                    steps {
                        ansiColor('xterm') {
                            sh 'mvn -t toolchains-jenkins-ubuntu.xml -Djenkins -V install'
                            junit '*/target/*-reports/*.xml'
                            stash includes: 'target/**', name: 'target'
                        }
                    }
                }
                stage('IBM JDK') {
                    agent { label 'ubuntu&&!H20' }
                    tools {
                        jdk 'IBM 1.8 64-bit (on Ubuntu only)'
                        maven 'Maven 3 (latest)'
                    }
                    steps {
                        ansiColor('xterm') {
                            sh 'mvn -t toolchains-jenkins-ibm.xml -Djenkins -V install'
                            junit '*/target/*-reports/*.xml'
                        }
                    }
                }
                stage('Windows') {
                    agent { label 'Windows' }
                    tools {
                        // https://cwiki.apache.org/confluence/display/INFRA/JDK+Installation+Matrix
                        jdk 'JDK 1.8 (latest)'
                        // https://cwiki.apache.org/confluence/display/INFRA/Maven+Installation+Matrix
                        maven 'Maven 3 (latest)'
                    }
                    steps {
                        bat 'if exist %userprofile%\\.embedmongo\\ rd /s /q %userprofile%\\.embedmongo'
                        bat 'mvn -t toolchains-jenkins-win.xml -V -Dfile.encoding=UTF-8 install'
                        junit '*/target/*-reports/*.xml'
                    }
                }
            }
        }
        stage('Deploy') {
            when { branch 'master' }
            tools {
                // https://cwiki.apache.org/confluence/display/INFRA/JDK+Installation+Matrix
                jdk 'JDK 1.8 (latest)'
                // https://cwiki.apache.org/confluence/display/INFRA/Maven+Installation+Matrix
                maven 'Maven 3 (latest)'
            }
            steps {
                ansiColor('xterm') {
                    unstash 'target'
                    sh 'mvn -t toolchains-jenkins-ubuntu.xml -Djenkins -DskipTests -V deploy'
                }
            }
//            post {
//                failure {
//                    emailext body: "See <${env.BUILD_URL}>", replyTo: 'dev@logging.apache.org', subject: "[Log4j] Jenkins build failure (#${env.BUILD_NUMBER})", to: 'notifications@logging.apache.org'
//                }
//            }
        }
    }
}
