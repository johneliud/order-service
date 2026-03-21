pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '3'))
        disableConcurrentBuilds()
        timestamps()
    }

    environment {
        SERVICE_NAME = 'order-service'
    }

    stages {
        stage('Initialize') {
            steps {
                echo "Starting build for ${env.SERVICE_NAME}..."
                sh 'mvn -version'
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo "Compiling ${env.SERVICE_NAME}..."
                sh 'mvn -B clean compile -DskipTests'
            }
        }

        stage('Unit Test') {
            steps {
                echo "Running JUnit tests for ${env.SERVICE_NAME}..."
                sh 'mvn -B test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                echo "Packaging ${env.SERVICE_NAME} into a JAR..."
                sh 'mvn -B package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                }
            }
        }

        stage('Deploy') {
            steps {
                echo "Deploying ${env.SERVICE_NAME} to Render..."
                // withCredentials([string(credentialsId: 'render-deploy-hook-media-service', variable: 'RENDER_DEPLOY_HOOK')]) {
                //     sh 'curl -X POST "$RENDER_DEPLOY_HOOK"'
                // }
            }
            post {
                failure {
                    echo "Deployment failed for ${env.SERVICE_NAME}. Rolling back..."
                    // withCredentials([string(credentialsId: 'render-deploy-hook-media-service', variable: 'RENDER_DEPLOY_HOOK')]) {
                    // Triggers a redeploy of the last pushed commit on the connected branch
                    // For version-specific rollback, use the Render dashboard: Dashboard > Service > Deploys > Redeploy
                    //     sh 'curl -X POST "$RENDER_DEPLOY_HOOK"'
                    // }
                }
            }
        }
    }

    post {
        success {
        	echo "SUCCESS: ${env.SERVICE_NAME} build and tests passed. Alert sent to email."
            emailext(
                subject: "[Jenkins] ${env.JOB_NAME} #${env.BUILD_NUMBER} — Build Successful",
                body: """
                    <p><b>Status:</b> SUCCESS</p>
                    <p><b>Service:</b> ${env.SERVICE_NAME}</p>
                    <p><b>Job:</b> ${env.JOB_NAME}</p>
                    <p><b>Build:</b> #${env.BUILD_NUMBER}</p>
                    <p><b>Console Output:</b> <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></p>
                """,
                mimeType: 'text/html',
                to: 'johneliud2001@gmail.com'
            )
        }
        failure {
        	echo "FAILURE: ${env.SERVICE_NAME} build or tests failed. Alert sent to email. Check logs and JUnit reports."
            emailext(
                subject: "[Jenkins] ${env.JOB_NAME} #${env.BUILD_NUMBER} — Build Failed",
                body: """
                    <p><b>Status:</b> FAILURE</p>
                    <p><b>Service:</b> ${env.SERVICE_NAME}</p>
                    <p><b>Job:</b> ${env.JOB_NAME}</p>
                    <p><b>Build:</b> #${env.BUILD_NUMBER}</p>
                    <p><b>Console Output:</b> <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></p>
                """,
                mimeType: 'text/html',
                to: 'johneliud2001@gmail.com'
            )
        }
        always {
            echo "Cleaning up workspace..."
            cleanWs()
        }
    }
}