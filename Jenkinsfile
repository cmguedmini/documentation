pipeline {
    agent {
        kubernetes {
            label 'react-pipeline'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: node
    image: node:18-alpine # Or your preferred Node.js image
    command:
    - cat
    tty: true
"""
        }
    }
    stages {
        stage('Build') {
            steps {
                container('node') {
                    sh 'pnpm install'
                    sh 'pnpm run build'
                }
            }
        }
        stage('Test') {
            steps {
                container('node') {
                   sh 'pnpm install' # ensure dependencies are installed
                   sh 'pnpm run test:e2e' # Assuming your Playwright tests are run with this command
                }
            }
        }
    }
    post {
        always {
            echo "Pipeline completed."
            script {
                // You can add more detailed logging here, e.g., capturing build artifacts or test results
                println "Build and test logs available in the console output."
            }
        }
    }
}
