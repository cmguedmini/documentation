# Deploy a React App to Azure with GitHub Actions CI/CD: Stress-Free, Smiles Included! 🚀
---

So you’ve built a React app (like this [ToDoList-React](https://github.com/cmguedmini/ToDoList-React.git) project) and want to automate deployments? This guide will show you how to deploy using Azure Blob Storage, Azure CDN, **and** the existing GitHub Actions workflow in the project. Let’s dive in!

---

## **What’s Special About This Setup?**
- **Zero Manual Deployments**: The built-in GitHub Actions workflow (`deploy.yml`) automates everything.
- **React Optimization**: Perfect for SPAs with client-side routing.
- **Azure CDN**: Global performance boost out of the box.

---
By the end, you’ll have:  
✅ A live React app hosted on Azure  
✅ A self-deploying robot (okay, a workflow)  
✅ More time to build cool stuff (or nap)  

Ready to turn deployment dread into delight? Let’s go! 🎈  
---

## **The GitHub Actions Workflow Explained**
The project already includes a CI/CD pipeline in [.github/workflows/deploy.yml](https://github.com/cmguedmini/ToDoList-React/blob/master/.github/workflows/deploy.yml). Let’s break down what it does:

### **1. Workflow Triggers**
```yaml
on:
  push:
    branches: [ "master" ]
  pull_request:
    branches: [ "master" ]
```
- Automatically runs on **pushes** or **pull requests** to the `master` branch.

### **2. Jobs and Steps**
```yaml
jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up Node.js
        uses: actions/setup-node@v3
        with:
          node-version: 18.x

      - name: Install dependencies and build
        run: |
          npm install
          npm run build

      - name: Install Azure CLI
        run: |
          curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

      - name: Login to Azure
        uses: azure/login@v1
        with:
          creds: ${{ secrets.AZURE_CREDENTIALS }}

      - name: Deploy to Azure Storage
        run: |
          az storage blob upload-batch \
            --account-name ${{ secrets.STORAGE_ACCOUNT_NAME }} \
            --destination "\$web" \
            --source "./build"
```

### **Key Stages:**
1. **Code Checkout**: Pulls the latest React code from GitHub.
2. **Node.js Setup**: Installs Node.js v18 for consistent builds.
3. **Build Process**: Installs dependencies and creates a production build.
4. **Azure CLI Setup**: Installs Azure CLI on the GitHub runner.
5. **Azure Authentication**: Uses stored credentials to log in.
6. **Blob Deployment**: Uploads the `build` folder to Azure Storage.

---

## **Why This CI/CD Workflow Rocks 🎸**
1. **Full Automation**: Push code → automatic build → automatic deploy.
2. **Consistency**: Eliminates "it works on my machine" issues.
3. **Security**: Azure credentials stored in GitHub Secrets (no exposure).
4. **Speed**: Deploys in ~2 minutes (see GitHub Actions logs).

---

## **How to Use This Workflow**
### **1. Configure Azure Credentials**
1. Create a service principal:
   ```bash
   az ad sp create-for-rbac --name "todo-react-cicd" --role contributor
   ```
2. Add these secrets to your GitHub repo:
   - `AZURE_CREDENTIALS`: Output from the service principal command
   - `STORAGE_ACCOUNT_NAME`: Your Azure storage account name

### **2. Let GitHub Actions Handle the Rest**
Just merge code to `master`, and the workflow will:
- Build your React app
- Deploy to Azure Blob Storage
- (Optional) Add CDN cache purge steps here!

---

## **Manual Deployment vs. CI/CD: Key Differences**
|                          | Manual Deployment           | GitHub Actions CI/CD       |
|--------------------------|-----------------------------|----------------------------|
| **Trigger**              | Run commands locally        | Automatic on code changes  |
| **Human Error Risk**     | Higher                      | Minimal                    |
| **Speed**                | Slower (5-10 mins)          | Faster (2-5 mins)          |
| **Best For**             | Initial setup/testing       | Production-ready workflows |

---

## **Enhancing the Workflow (Pro Tips)**
1. **Add CDN Cache Purging**:
   ```yaml
   - name: Purge CDN Cache
     run: |
       az cdn endpoint purge \
         --name "todo-cdn-endpoint" \
         --profile-name "todo-cdn-profile" \
         --resource-group "todo-react-rg" \
         --content-paths "/*"
   ```
2. **Add Slack Notifications**: Get alerts for failed deployments.
3. **Run Tests**: Add `npm test` before building.

---

## **Troubleshooting CI/CD**
- **Permission Errors**: Double-check `AZURE_CREDENTIALS` format.
- **Build Failures**: Test locally with `npm run build`.
- **404 Errors**: Ensure `404-document` is set to `index.html` in Azure.

---

## **Final Thoughts**
By leveraging the existing `deploy.yml` workflow, you’ve transformed deployment from a manual chore to an automated superpower. This setup is ideal for:
- Solo developers wanting efficiency
- Teams needing reliable deployments
- Projects requiring frequent updates

**Next Steps**:
- Add staging/production environments
- Implement branch protection rules
- Explore Azure Static Web Apps for full-stack React apps

Found this helpful? Star the [ToDoList-React repo](https://github.com/cmguedmini/ToDoList-React.git) and share this guide! 🌟  
Questions? Ask below! 👇  
``` 
