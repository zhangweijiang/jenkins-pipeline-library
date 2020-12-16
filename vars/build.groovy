#!/usr/bin/env groovy

def getServer() {
    def remote = [:]
    remote.name = 'manager node'
    remote.user = 'dev'
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
            DOCKER_HOST = "registry-vpc.cn-hangzhou.aliyuncs.com"
            IMG_NAME = "shzhyt/test"
            DOCKER_IMAGE_NAME = "${docker_host}/${img_name}"
            BUILD_TAG = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
        }

        stages {
           
            stage('Build') {
                steps {
                     sh "echo ${BUILD_TAG}"
                     sh "echo ${DOCKER_IMAGE_NAME}"
                     sh "docker build -t ${DOCKER_IMAGE_NAME}:${BUILD_TAG} ./"
                }
            }
            
            stage('Push') {
                steps {
                     sh "docker tag ${DOCKER_IMAGE_NAME}:${BUILD_TAG} ${docker_img_name}:latest"
                     withCredentials([usernamePassword(credentialsId: 'docker-register', passwordVariable: 'dockerPassword', usernameVariable: 'dockerUser')]) {
                         sh "docker login -u ${dockerUser} -p ${dockerPassword} registry-vpc.cn-hangzhou.aliyuncs.com"
                         sh "docker push ${DOCKER_IMAGE_NAME}:latest"
                         sh "docker push ${DOCKER_IMAGE_NAME}:${BUILD_TAG}"
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
                    //writeFile file: 'deploy.sh', text: "wget -O ${COMPOSE_FILE_NAME} " +
                            //" https://git.x-vipay.com/docker/jenkins-pipeline-library/raw/master/resources/docker-compose/${COMPOSE_FILE_NAME} \n" +
                            //"sudo docker stack deploy -c ${COMPOSE_FILE_NAME} ${STACK_NAME}"
                    sshScript remote: server, script: "/root/start.sh"
                }
            }
        }
    }
}
