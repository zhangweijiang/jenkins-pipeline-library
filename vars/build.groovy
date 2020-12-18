#!/usr/bin/env groovy

def getServer() {
    def remote = [:]
    remote.name = 'manager node'
    remote.user = 'root'
    remote.host = "${REMOTE_HOST}"
    remote.port = 22
    remote.identityFile = '/root/.ssh/id_rsa'
    remote.allowAnyHosts = true
    return remote
}

def call(Map map) {

    pipeline {
        agent any

        environment {
            REMOTE_HOST = "${map.REMOTE_HOST}"
            REPO_URL = "${map.REPO_URL}"
            BRANCH_NAME = "${map.BRANCH_NAME}"
            STACK_NAME = "${map.STACK_NAME}"
            COMPOSE_FILE_NAME = "docker-compose-" + "${map.STACK_NAME}" + "-" + "${map.BRANCH_NAME}" + ".yml"
            IMG_NAME = "shzhyt/test"
            DOCKER_IMAGE_NAME = "registry-vpc.cn-hangzhou.aliyuncs.com/shzhyt/test"
            BUILD_TAG = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
        }

        stages {
           
            stage('Build') {
                steps {            
                     sh "docker build -t ${DOCKER_IMAGE_NAME}:${BUILD_TAG} ./"
                }
            }
            
            stage('Push') {
                steps {
                     sh "docker tag ${DOCKER_IMAGE_NAME}:${BUILD_TAG} ${DOCKER_IMAGE_NAME}:latest"
                     withCredentials([usernamePassword(credentialsId: 'docker-register', passwordVariable: 'dockerPassword', usernameVariable: 'dockerUser')]) {
                        sh "docker login -u ${dockerUser} -p ${dockerPassword} registry-vpc.cn-hangzhou.aliyuncs.com"
                        sh "docker push ${DOCKER_IMAGE_NAME}:latest"
                        sh "docker push ${DOCKER_IMAGE_NAME}:${BUILD_TAG}"
                        sh "docker rmi ${DOCKER_IMAGE_NAME}:${BUILD_TAG}"
                     }
                }
            }

            stage('init-server') {
                steps {
                    script {
                        server = getServer()
                    }
                }
            }

            stage('执行发版') {
                steps {
                    sh "cd ./deploy && chmod a+x ./build.sh && ./build.sh ${REMOTE_HOST} ${STACK_NAME}"
                }
            }
        }
    }
}
