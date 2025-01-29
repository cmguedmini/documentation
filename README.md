Certainly! Below is a **README.md** template for setting up a Jenkins pipeline for running **E2E tests** with **Playwright** and deploying a **React app** to **Azure Blob Storage**. I've also included explanations and diagrams to make it easy to understand and follow.

```markdown
# Jenkins Pipeline for E2E Testing and Azure Blob Storage Deployment

This README explains how to set up a **Jenkins pipeline** for:
1. Running **E2E tests** for a **React app** using **Playwright**.
2. Deploying the **React app** to **Azure Blob Storage**.

## Requirements

### 1. **Jenkins Setup**
- **Jenkins Agent**: Ensure the Jenkins agent where the pipeline will run has **Node.js**, **pnpm**, and **Azure CLI** installed.
- **Jenkins Plugins**:
  - **Pipeline** (Default in Jenkins)
  - **Credentials Plugin** (For securely storing Azure credentials)
  - Optionally: **Azure CLI Plugin** (for better integration with Azure CLI commands)

### 2. **Azure Setup**
- **Azure Storage Account**: A storage account in Azure with a Blob container for storing the React app's build.
- **Azure Service Principal**: A service principal with permissions to access Azure Blob Storage.
- **Azure Storage Account Key**: Key for accessing the Blob Storage account.

### 3. **Jenkins Credentials**
- **Azure Service Principal Credentials**: Store `client_id`, `client_secret`, and `tenant_id` in Jenkins as a secret.
- **Azure Storage Account Key**: Store the `account_key` of your Azure Storage in Jenkins as a secret.

## Jenkins Pipeline Overview

The pipeline consists of the following stages:
1. **Checkout**: Checkout the source code from version control.
2. **Install pnpm**: Install `pnpm` (a fast package manager) globally.
3. **Install Dependencies**: Install dependencies in the `app` folder using `pnpm`.
4. **Build React App**: Build the React app in the `app` folder.
5. **Run Playwright E2E Tests**: Run Playwright tests located in the `e2e` folder.
6. **Deploy to Azure Blob Storage**: Upload the build files to Azure Blob Storage, conditional on the `DEPLOY_TO_AZURE` parameter.

### Flow Diagram

```plaintext
+-----------------+      +---------------------+      +-------------------------+
|                 |      |                     |      |                         |
| Checkout Code   |----->| Install Dependencies |----->| Build React Application |
| (Git Repo)      |      | (app folder)         |      | (app folder)             |
|                 |      |                     |      |                         |
+-----------------+      +---------------------+      +-------------------------+
                                                              |
                                                              v
                                                     +---------------------+
                                                     | Run Playwright E2E  |
                                                     | Tests (e2e folder)  |
                                                     +---------------------+
                                                              |
                     +----------------------------------------+
                     |
                     v
        +---------------------------------------------+
        | Conditional Deployment to Azure Blob Storage|
        | (if DEPLOY_TO_AZURE is true)                |
        +---------------------------------------------+
                     |
                     v
         +-----------------------------+
         | Upload to Azure Blob Storage|
         | (app/build to Blob Storage) |
         +-----------------------------+
```

## Jenkins Pipeline Example

Here’s a Jenkins pipeline `Jenkinsfile` that performs the steps described above:

```groovy
pipeline {
    agent any

    parameters {
        booleanParam(name: 'DEPLOY_TO_AZURE', defaultValue: false, description: 'Set to true to deploy the React app to Azure Blob Storage')
    }

    environment {
        NODE_HOME = tool name: 'NodeJS', type: 'NodeJSInstallation'
        PATH = "${NODE_HOME}/bin:${env.PATH}"
        AZURE_STORAGE_ACCOUNT = 'your-storage-account-name'
        AZURE_CONTAINER_NAME = 'your-container-name'
        AZURE_STORAGE_KEY = credentials('azure-storage-key') 
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Install pnpm') {
            steps {
                script {
                    sh 'npm install -g pnpm'
                }
            }
        }

        stage('Install dependencies') {
            steps {
                script {
                    dir('app') {
                        sh 'pnpm install'
                    }
                }
            }
        }

        stage('Build React App') {
            steps {
                script {
                    dir('app') {
                        sh 'pnpm run build'
                    }
                }
            }
        }

        stage('Run Playwright E2E Tests') {
            steps {
                script {
                    dir('e2e') {
                        sh 'npx playwright install'
                        sh 'pnpm run test:e2e'
                    }
                }
            }
        }

        stage('Deploy to Azure Blob Storage') {
            when {
                expression { params.DEPLOY_TO_AZURE == true }
            }
            steps {
                script {
                    sh 'curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash'
                    withCredentials([azureServicePrincipal(credentialsId: 'azure-sp-credentials')]) {
                        sh '''
                            az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET --tenant $AZURE_TENANT_ID
                        '''
                    }

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
            echo 'Pipeline completed successfully.'
        }
        failure {
            echo 'Pipeline failed.'
        }
    }
}
```

### Explanation of Key Steps:
- **Checkout**: Fetches the source code from your repository.
- **Install pnpm**: Ensures `pnpm` is installed globally.
- **Install dependencies**: Installs the project dependencies in the `app` directory using `pnpm`.
- **Build React App**: Runs the build process in the `app` folder to create the production-ready files.
- **Run Playwright E2E Tests**: Executes Playwright E2E tests located in the `e2e` folder.
- **Deploy to Azure Blob Storage**: Conditionally uploads the build folder to Azure Blob Storage if `DEPLOY_TO_AZURE` is set to `true`.

### Azure Configuration:
1. **Azure Storage Account**: Create a **Blob Storage** container in your Azure portal to host your React app.
2. **Azure Service Principal**: Create a **Service Principal** in Azure AD and grant it access to your Blob Storage. Store the credentials (`client_id`, `client_secret`, and `tenant_id`) in Jenkins as **Azure Service Principal credentials**.

## Setting Up Jenkins Credentials:
1. **Azure Service Principal Credentials**: 
   - Go to **Manage Jenkins > Manage Credentials**.
   - Add a new **Azure Service Principal** credential containing the `client_id`, `client_secret`, and `tenant_id`.
   - Add the **Azure Storage Account Key** in the same way.

2. **Configure Jenkins Node**: Ensure the Jenkins node (or agent) where the pipeline runs has the following installed:
   - **Node.js**
   - **pnpm** (via `npm install -g pnpm`)
   - **Azure CLI** (via `curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash`)

## Conclusion
By following these steps, you’ll have a fully automated Jenkins pipeline that:
1. Builds a **React app** in the `app` directory.
2. Runs **Playwright** E2E tests located in the `e2e` directory.
3. Optionally deploys the app to **Azure Blob Storage** depending on the `DEPLOY_TO_AZURE` parameter.

Let me know if you have any further questions or need additional details!
```

### Key Sections in the README:
1. **Requirements**: Describes the prerequisites for the Jenkins setup and Azure configuration.
2. **Pipeline Overview**: Provides a high-level overview of the pipeline stages with a flow diagram.
3. **Jenkinsfile**: The actual Jenkins pipeline script with all the required steps and logic for the pipeline.
4. **Azure Configuration**: Details on how to set up Azure resources like Blob Storage and Service Principal.
5. **Setting Up Jenkins Credentials**: Instructions on how to securely store Azure credentials in Jenkins.

This README provides a comprehensive guide to setting up the pipeline with diagrams and all necessary details. Let me know if you'd like to refine any part or add additional sections!
