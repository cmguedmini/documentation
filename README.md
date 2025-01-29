To conditionally activate the "Deploy to Azure Blob Storage" stage based on a boolean property, you can add a **parameter** to the pipeline. Jenkins provides a `parameters` block that allows you to define input parameters. We can add a boolean parameter to control whether the deployment step is executed.

Here’s how you can update the pipeline:

### Updated Jenkinsfile with Deploy Stage Controlled by Boolean Property:

```groovy
pipeline {
    agent any

    parameters {
        // Define a boolean parameter to control deployment to Azure Blob Storage
        booleanParam(name: 'DEPLOY_TO_AZURE', defaultValue: false, description: 'Set to true to deploy the React app to Azure Blob Storage')
    }

    environment {
        // You can define environment variables here
        NODE_HOME = tool name: 'NodeJS', type: 'NodeJSInstallation'
        PATH = "${NODE_HOME}/bin:${env.PATH}"

        // Azure Storage Account Info
        AZURE_STORAGE_ACCOUNT = 'your-storage-account-name'
        AZURE_CONTAINER_NAME = 'your-container-name'
        AZURE_STORAGE_KEY = credentials('azure-storage-key') // Store the Azure storage key as Jenkins secret
    }

    stages {
        stage('Checkout') {
            steps {
                // Checkout the repository
                checkout scm
            }
        }

        stage('Install pnpm') {
            steps {
                // Install pnpm globally if not already installed
                script {
                    sh 'npm install -g pnpm'
                }
            }
        }

        stage('Install dependencies') {
            steps {
                // Install dependencies in the 'app' folder using pnpm
                script {
                    dir('app') {
                        sh 'pnpm install'
                    }
                }
            }
        }

        stage('Build React App') {
            steps {
                // Build the React application in the 'app' folder
                script {
                    dir('app') {
                        sh 'pnpm run build'
                    }
                }
            }
        }

        stage('Run Playwright E2E Tests') {
            steps {
                // Run Playwright tests located in the 'e2e' folder
                script {
                    dir('e2e') {
                        sh 'npx playwright install'  // Install Playwright browsers
                        sh 'pnpm run test:e2e'       // Run Playwright E2E tests
                    }
                }
            }
        }

        stage('Deploy to Azure Blob Storage') {
            when {
                // The deploy stage will run only if the boolean parameter DEPLOY_TO_AZURE is true
                expression { params.DEPLOY_TO_AZURE == true }
            }
            steps {
                script {
                    // Install Azure CLI if it's not already installed
                    sh 'curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash'

                    // Login to Azure using a service principal (ensure you have set the credentials)
                    withCredentials([azureServicePrincipal(credentialsId: 'azure-sp-credentials')]) {
                        sh '''
                            az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET --tenant $AZURE_TENANT_ID
                        '''
                    }

                    // Upload the build folder from the 'app' directory to Azure Blob Storage
                    sh """
                        az storage blob upload-batch \
                            --account-name $AZURE_STORAGE_ACCOUNT \
                            --destination \$AZURE_CONTAINER_NAME \
                            --source ./app/build \
                            --pattern '*' \
                            --auth-mode key \
                            --account-key \$AZURE_STORAGE_KEY
                    """
                }
            }
        }
    }

    post {
        success {
            // Notify or perform actions on success
            echo 'Pipeline completed successfully.'
        }
        failure {
            // Handle failure (e.g., send notifications)
            echo 'Pipeline failed.'
        }
    }
}
```

### Explanation of the Update:
1. **Boolean Parameter (`DEPLOY_TO_AZURE`)**:
   - A `booleanParam` is defined at the top of the pipeline in the `parameters` block:
     ```groovy
     booleanParam(name: 'DEPLOY_TO_AZURE', defaultValue: false, description: 'Set to true to deploy the React app to Azure Blob Storage')
     ```
   - This creates a parameter named `DEPLOY_TO_AZURE` that defaults to `false`. When the pipeline is triggered, you can set this to `true` to activate the deployment step.

2. **Conditional Execution of the Deployment Stage**:
   - The `Deploy to Azure Blob Storage` stage is wrapped with a `when` block that checks the value of `DEPLOY_TO_AZURE`:
     ```groovy
     when {
         expression { params.DEPLOY_TO_AZURE == true }
     }
     ```
   - This ensures that the "Deploy to Azure Blob Storage" stage will only run if `DEPLOY_TO_AZURE` is set to `true`.

### How It Works:
- **When Triggering the Pipeline**: If you're manually triggering the pipeline, Jenkins will prompt you to select the value of the `DEPLOY_TO_AZURE` parameter.
- **If `DEPLOY_TO_AZURE` is `true`**: The pipeline will proceed with the "Deploy to Azure Blob Storage" stage and upload the build files to Azure.
- **If `DEPLOY_TO_AZURE` is `false`**: The pipeline will skip the deployment step and finish after running the tests.

### Triggering the Pipeline:
- If you manually trigger the pipeline through Jenkins, you'll have the option to select `DEPLOY_TO_AZURE` as `true` or `false`.
- If you want to trigger the pipeline automatically and set the parameter programmatically, you can use Jenkins API or a script to pass the value.

### Example:
When manually triggering the pipeline in Jenkins, you’ll see a prompt like this:

```
DEPLOY_TO_AZURE: [ ] true
```

You can check the box to set it to `true`, which will trigger the deployment to Azure Blob Storage.

### Conclusion:
This updated Jenkins pipeline will conditionally deploy the React app to Azure Blob Storage based on the `DEPLOY_TO_AZURE` boolean parameter. You can use this flexibility to control whether or not to deploy, without modifying the pipeline code every time. Let me know if you need further adjustments!
