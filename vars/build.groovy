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
            BUILD_TAG = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
        }

        stages {
            stage('Prepare') {
                steps {
                    sh "echo docker-img-name: ${docker_img_name}"
                }
            }
            stage('Test') {
                steps {
                    sh "echo "2.Test Stage"
                }
            }

            stage('Build') {
                steps {
                     sh "echo 3.Build Docker Image Stage"
                     sh "docker build -t ${docker_img_name}:${build_tag} ./"
                }
            }
            
            stage('Push') {
                steps {
                     sh "echo 4.Deploy jar and Push Docker Image Stage"
                     sh "docker tag ${docker_img_name}:${build_tag} ${docker_img_name}:latest"
                     withCredentials([usernamePassword(credentialsId: 'docker-register', passwordVariable: 'dockerPassword', usernameVariable: 'dockerUser')]) {
                         sh "docker login -u ${dockerUser} -p ${dockerPassword} registry-vpc.cn-hangzhou.aliyuncs.com"
                         sh "docker push ${docker_img_name}:latest"
                         sh "docker push ${docker_img_name}:${build_tag}"
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
                    writeFile file: 'deploy.sh', text: "wget -O ${COMPOSE_FILE_NAME} " +
                            " https://git.x-vipay.com/docker/jenkins-pipeline-library/raw/master/resources/docker-compose/${COMPOSE_FILE_NAME} \n" +
                            "sudo docker stack deploy -c ${COMPOSE_FILE_NAME} ${STACK_NAME}"
                    sshScript remote: server, script: "deploy.sh"
                }
            }
        }
    }
}
