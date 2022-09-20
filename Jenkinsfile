pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                echo 'Building..'
                cleanWs()
                sh 'git clone https://github.com/Lukk17/sky.git'
                sh './auth-service/gradlew clean compile'
            }
        }
        stage('Test') {
            steps {
                echo 'Testing..'
                sh './auth-service/gradlew test'
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying....'
                sh 'docker-compose -f ./config/docker/docker-compose.yml up'
            }
        }
    }
}