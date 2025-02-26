podTemplate(label: 'react-pipeline', containers: [
    containerTemplate(name: 'node', image: 'node:18-alpine', ttyEnabled: true, command: 'cat')
]) {
    node('react-pipeline') {
        stage('Build') {
            container('node') {
                sh 'pnpm install'
                sh 'pnpm run build'
            }
        }
        stage('Test') {
            container('node') {
                sh 'pnpm install'
                sh 'pnpm run test:e2e'
            }
        }
        post {
            always {
                echo "Pipeline completed."
                script {
                    println "Build and test logs available in the console output."
                }
            }
        }
    }
}
