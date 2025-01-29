To create a React application that follows the pipeline setup we discussed (including Playwright for E2E testing and deployment to Azure Blob Storage), I’ll walk you through the entire process. The steps below will cover the necessary dependencies, structure, and configurations for both the React app and the necessary pipeline.

### Step 1: Set Up Your React Application

First, let’s create the basic React application structure.

#### 1. **Create the React App:**
You can create a new React app using **Create React App** and install the required dependencies.

```bash
# Create a new React app
npx create-react-app react-playwright-app

# Navigate to the project folder
cd react-playwright-app
```

#### 2. **Install the Required Dependencies:**

In the project, we need to install a few dependencies, including `pnpm`, **Playwright** for E2E testing, and the necessary Azure deployment tools.

- Install **pnpm** globally (if you don’t have it yet):
  ```bash
  npm install -g pnpm
  ```

- Install dependencies with **pnpm** (e.g., Playwright, axios for API calls, etc.):
  ```bash
  pnpm install
  ```

- Add **Playwright** as a dev dependency for testing:
  ```bash
  pnpm add -D playwright
  ```

- For the Azure Blob Storage deployment (using Azure CLI tools), we will configure this later in Jenkins.

#### 3. **React Application Code (Example)**

Here’s a simple **React app** with a basic component to test in the Playwright E2E tests:

- **`src/App.js`:**

```javascript
import React, { useState } from 'react';
import './App.css';

function App() {
  const [count, setCount] = useState(0);

  return (
    <div className="App">
      <header className="App-header">
        <h1>React Playwright App</h1>
        <button onClick={() => setCount(count + 1)}>
          Count: {count}
        </button>
      </header>
    </div>
  );
}

export default App;
```

This is just a simple button that increases a count to use for testing purposes.

---

### Step 2: Create Playwright E2E Tests

Next, let’s set up **Playwright** to perform E2E testing for our React app.

#### 1. **Set Up Playwright E2E Tests:**

In the root of your project, create a folder `e2e/` to store the Playwright tests.

- **`e2e/example.spec.js`:**

```javascript
const { test, expect } = require('@playwright/test');

test('should display the correct count', async ({ page }) => {
  // Navigate to the React app
  await page.goto('http://localhost:3000');

  // Click the button to increase the count
  await page.click('button');

  // Verify the count is incremented
  const button = await page.locator('button');
  await expect(button).toHaveText('Count: 1');
});
```

This is a basic Playwright test that ensures the button on the page correctly increments the count when clicked.

#### 2. **Install Playwright Browsers:**

Since Playwright supports Chromium, Firefox, and WebKit, you'll need to install the browsers for Playwright.

```bash
pnpx playwright install
```

---

### Step 3: Set Up Azure Blob Storage Deployment

To deploy to Azure Blob Storage, you’ll need to follow these steps (in the Jenkins pipeline, as we discussed earlier).

Here’s the process for deploying a React app to Azure Blob Storage:

#### 1. **Azure Setup:**
- **Create a Storage Account** and a **Blob Container** in your Azure portal.
- Set up a **Service Principal** for authentication, as mentioned earlier. This will allow Jenkins to deploy the application to Azure Blob Storage.

#### 2. **Azure Deployment Script** (in Jenkinsfile):

You can use the following deployment script in your Jenkins pipeline to upload the build to Azure Blob Storage:

```bash
az storage blob upload-batch \
  --account-name <your_storage_account> \
  --destination <your_container_name> \
  --source ./build \
  --pattern '*' \
  --auth-mode key \
  --account-key $AZURE_STORAGE_KEY
```

This script will upload all the build files from the `./build` folder to your Azure Blob Storage container.

---

### Step 4: Create Jenkinsfile for CI/CD Pipeline

The Jenkinsfile defines the CI/CD pipeline, automating the build, test, and deployment process.

```groovy
pipeline {
    agent any

    parameters {
        booleanParam(name: 'DEPLOY_TO_AZURE', defaultValue: false, description: 'Deploy the React app to Azure Blob Storage')
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

#### Notes:
- Replace `your-storage-account-name`, `your-container-name`, and `AZURE_STORAGE_KEY` with your actual Azure credentials.
- This pipeline will build the React app, run Playwright tests, and optionally deploy to Azure Blob Storage based on the boolean parameter `DEPLOY_TO_AZURE`.

---

### Step 5: Running the Project Locally

1. **Start the React App:**
   You can start your React app locally to test it before pushing it to Jenkins.

   ```bash
   pnpm start
   ```

2. **Run Playwright Tests Locally:**
   You can also run Playwright tests locally to verify that everything works:

   ```bash
   pnpx playwright test
   ```

3. **Build the App:**
   Once everything looks good, build the React app:

   ```bash
   pnpm run build
   ```

---

### Final Folder Structure:

Your project folder structure will look like this:

```
react-playwright-app/
├── app/
│   ├── src/
│   │   ├── App.js
│   │   ├── index.js
│   │   └── ... (other React files)
│   ├── public/
│   └── package.json (pnpm-lock.yaml)
├── e2e/
│   ├── example.spec.js
├── Jenkinsfile
└── README.md
```

---

### Conclusion

You now have a **React application** set up with the following:
1. **Playwright** for E2E testing.
2. **Azure Blob Storage** deployment via Jenkins.
3. A fully automated **CI/CD pipeline** using Jenkins.

With this setup, you can ensure your React app is built, tested, and deployed seamlessly with every change.

Feel free to customize the pipeline and test cases based on your project’s requirements! Let me know if you need any further clarification or adjustments.
