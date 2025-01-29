# **How to Set Up a Jenkins Pipeline for E2E Testing and Azure Blob Storage Deployment**

## **Introduction**

In modern development workflows, automation is key to ensuring rapid, reliable, and consistent deployments. **Jenkins**, one of the most popular CI/CD tools, allows us to automate tasks like building, testing, and deploying applications. If you're working with a **React app** and want to streamline the process with **end-to-end (E2E) tests** and **cloud deployment**, you’ve come to the right place.

This guide will walk you through setting up a **Jenkins pipeline** that:
1. Runs **E2E tests** for your React app using **Playwright**.
2. Deploys the production-ready build of your React app to **Azure Blob Storage**.

By the end of this post, you'll have a robust pipeline that automatically builds, tests, and deploys your app, ensuring faster feedback and smoother releases.

For a more hands-on example, you can access the full source code [here](https://github.com/cmguedmini/documentation/tree/main).

---

## **Prerequisites**

Before jumping into the pipeline setup, make sure you have the following:

### 1. **Jenkins Setup**
- **Jenkins agent** with **Node.js** and **pnpm** installed.
- **Azure CLI** installed on the agent for interacting with **Azure Blob Storage**.

### 2. **Azure Setup**
- **Azure Storage Account** and a **Blob container** to store your app’s build.
- **Azure Service Principal** for authenticating Jenkins during uploads to Azure Blob Storage.

### 3. **Jenkins Plugins**
Make sure you have these Jenkins plugins installed:
- **Pipeline Plugin** (for defining the pipeline).
- **Credentials Plugin** (for securely storing Azure credentials).

### 4. **Create Jenkins Credentials**
You’ll need to store:
- **Azure Service Principal credentials** (client_id, client_secret, tenant_id).
- **Azure Storage Account Key** to access Blob Storage.

---

## **The Jenkins Pipeline**

Now let’s take a look at how the pipeline itself works. We’ll break it down into its main stages, and then I’ll show you the full **Jenkinsfile**.

### **Pipeline Stages:**
1. **Checkout**: Pulls the latest code from the repository.
2. **Install pnpm**: Installs **pnpm** (our package manager).
3. **Install Dependencies**: Runs `pnpm install` to install all dependencies in the `app` folder.
4. **Build React App**: Executes `pnpm run build` to create the production-ready build.
5. **Run Playwright E2E Tests**: Runs Playwright E2E tests located in the `e2e` folder.
6. **Deploy to Azure Blob Storage**: Optionally deploys the React app to Azure Blob Storage, based on a parameter.

### **Jenkinsfile Example**

Here’s the full **Jenkinsfile** that defines the pipeline:

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

### **Breaking Down the Stages**

1. **Checkout**: 
   This stage checks out the latest code from your Git repository.

2. **Install pnpm**:
   Installs **pnpm** globally so we can use it to manage dependencies for our React app.

3. **Install Dependencies**:
   Runs `pnpm install` inside the `app` directory to install all dependencies for the React app.

4. **Build React App**:
   Executes `pnpm run build` to generate the production-ready build of your React app.

5. **Run Playwright E2E Tests**:
   - Installs **Playwright** using `npx playwright install`.
   - Runs Playwright E2E tests to verify the app’s behavior (`pnpm run test:e2e`).

6. **Deploy to Azure Blob Storage**:
   - This step uploads the build files to **Azure Blob Storage**.
   - It only runs if the `DEPLOY_TO_AZURE` parameter is set to `true`.

---

## **Setting Up Azure and Jenkins**

### **1. Set Up Azure Blob Storage**
To host your React app, follow these steps:
1. Go to the [Azure portal](https://portal.azure.com/).
2. Create a **Storage Account**.
3. Under **Containers**, create a **Blob container** to store your app’s build files.

### **2. Set Up Azure Service Principal**
Create an **Azure Service Principal** to authenticate Jenkins when uploading files to Blob Storage. Use the Azure CLI:

```bash
az ad sp create-for-rbac --name "JenkinsApp" --role contributor --scopes /subscriptions/{subscription-id}/resourceGroups/{resource-group-name}
```

This will give you the `client_id`, `client_secret`, and `tenant_id` to use in Jenkins.

---

## **Running the Pipeline**

### **1. Trigger the Pipeline**
When triggering the pipeline in Jenkins, you’ll be asked to set the `DEPLOY_TO_AZURE` parameter:
- If you set it to `true`, Jenkins will upload the built React app to Azure Blob Storage.
- If you set it to `false`, the deploy stage will be skipped.

### **2. Monitor Pipeline Execution**
Jenkins will show logs for each step:
- Installing dependencies
- Building the React app
- Running the Playwright tests
- Uploading the build to Azure (if applicable)

---

## **Conclusion**

You’ve now set up a fully automated **CI/CD pipeline** in Jenkins that:
1. Builds your **React app** with **pnpm**.
2. Runs **Playwright** end-to-end tests to ensure quality.
3. Optionally deploys the build to **Azure Blob Storage**.

This setup will help you automate your workflow, ensuring that every change to your app is tested and deployed seamlessly.

For the complete source code, check out the [repository here](https://github.com/cmguedmini/documentation/tree/main).

Let me know if you have any questions or need further assistance!

---

### Tags:
#Jenkins #CI #CD #Azure #BlobStorage #React #Playwright #E2ETests #DevOps #Automation

---

This updated version now includes an introduction to give readers context and set expectations for the rest of the blog. It should be more engaging and informative from the start! Let me know if you'd like to make further adjustments.
