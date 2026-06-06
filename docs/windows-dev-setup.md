# Windows Development Environment Setup

This guide walks you through installing every prerequisite needed to build and run the **JP Photo Manager** project (both the WPF desktop app and the Spring Boot / Angular web application) on a Windows machine.

---

## Table of Contents

1. [Git](#1-git)
2. [Chocolatey (Windows Package Manager)](#2-chocolatey-windows-package-manager)
3. [Java Development Kit 21](#3-java-development-kit-21)
4. [Apache Maven](#4-apache-maven)
5. [Node.js 22 LTS](#5-nodejs-22-lts)
6. [Angular CLI](#6-angular-cli)
7. [.NET 8 SDK](#7-net-8-sdk)
8. [Docker Desktop](#8-docker-desktop)
9. [IDE / Editors](#9-ide--editors)
   - [Visual Studio 2022 (WPF desktop app)](#visual-studio-2022-wpf-desktop-app)
   - [IntelliJ IDEA (Spring Boot backend)](#intellij-idea-spring-boot-backend)
   - [Visual Studio Code (Angular frontend)](#visual-studio-code-angular-frontend)
10. [Verify Your Installation](#10-verify-your-installation)

---

## 1. Git

Git is required to clone the repository and manage source control.

**Download:** https://git-scm.com/download/win

1. Run the installer (`Git-<version>-64-bit.exe`).
2. Accept the defaults on every screen **except** the following recommended changes:
   - **Default editor**: choose your preferred editor (e.g., Visual Studio Code).
   - **Adjusting PATH**: select **"Git from the command line and also from 3rd-party software"**.
   - **Line ending conversions**: select **"Checkout Windows-style, commit Unix-style line endings"**.
3. Finish the installation and open a new **Command Prompt** or **PowerShell** window.
4. Verify:

```powershell
git --version
# Expected: git version 2.x.x.windows.x
```

---

## 2. Chocolatey (Windows Package Manager)

Chocolatey is a command-line package manager for Windows that simplifies installing and updating developer tools.

### Option A – Install via PowerShell (recommended)

Open **PowerShell as Administrator** and run:

```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

### Option B – Download the MSI installer

An MSI installer for Chocolatey GUI (a graphical front-end) is available at:
https://community.chocolatey.org/packages/ChocolateyGUI

Verify Chocolatey is installed:

```powershell
choco --version
# Expected: 2.x.x
```

> **Tip:** After any `choco install` command, either restart your terminal or run `refreshenv` so the newly added paths are visible.

---

## 3. Java Development Kit 21

The Spring Boot backend targets **Java 21** (LTS).

### Option A – Install via Chocolatey

```powershell
choco install microsoft-openjdk21 -y
```

> This installs Microsoft's build of OpenJDK 21, which is the recommended distribution for Windows.

### Option B – Download manually

Download the **Microsoft Build of OpenJDK 21** MSI installer from:
https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-21

Or download **Eclipse Temurin 21** (Adoptium) from:
https://adoptium.net/temurin/releases/?version=21

1. Run the installer.
2. **Check the box** "Set or override JAVA_HOME variable" and "Add to PATH" if prompted.
3. Verify:

```powershell
java -version
# Expected: openjdk version "21.x.x" ...

echo %JAVA_HOME%
# Expected: C:\Program Files\Microsoft\jdk-21.x.x.x-hotspot  (or similar)
```

### Setting JAVA_HOME manually (if not set by the installer)

```powershell
# In PowerShell (Administrator)
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Microsoft\jdk-21.0.x.x-hotspot", "Machine")
[System.Environment]::SetEnvironmentVariable("Path", $env:Path + ";%JAVA_HOME%\bin", "Machine")
```

---

## 4. Apache Maven

Maven is the build and dependency management tool for the Java backend.

### Option A – Install via Chocolatey

```powershell
choco install maven -y
```

### Option B – Download manually

Download the latest **Apache Maven 3.x** binary ZIP from:
https://maven.apache.org/download.cgi

1. Extract the ZIP to a location such as `C:\tools\apache-maven-3.x.x`.
2. Add `C:\tools\apache-maven-3.x.x\bin` to your **Path** environment variable:
   - Open **System Properties** → **Advanced** → **Environment Variables**.
   - Under **System variables**, select **Path** → **Edit** → **New**, and add the `bin` path.
3. Verify:

```powershell
mvn -version
# Expected: Apache Maven 3.x.x ...
#           Java version: 21.x.x
```

> Maven requires `JAVA_HOME` to be set (see [section 3](#3-java-development-kit-21)).

---

## 5. Node.js 22 LTS

Node.js is required to build and run the Angular 19 frontend. The project requires **Node.js 22 LTS** (or 20 LTS as a minimum).

### Option A – Install via Chocolatey

```powershell
choco install nodejs-lts -y
```

> This always installs the current LTS release. As of 2025, that is Node.js 22.

### Option B – Download manually

Download the **Node.js 22 LTS** Windows installer (`.msi`) from:
https://nodejs.org/en/download/

1. Run the installer.
2. On the **Tools for Native Modules** screen, check **"Automatically install the necessary tools"** to also install Python and Visual C++ Build Tools (useful for native npm packages).
3. Verify:

```powershell
node --version
# Expected: v22.x.x

npm --version
# Expected: 10.x.x
```

> Installing Node.js via the `.msi` also installs **npm** (Node Package Manager) automatically.

---

## 6. Angular CLI

The Angular CLI (`ng`) is the command-line tool for generating, building, and serving Angular applications.

Install globally via npm after Node.js is installed:

```powershell
npm install -g @angular/cli@19
```

Verify:

```powershell
ng version
# Expected: Angular CLI: 19.x.x
#           Node: 22.x.x
```

---

## 7. .NET 8 SDK

The WPF desktop application targets **.NET 8.0**. The SDK includes the runtime, compiler, and CLI tools.

### Option A – Install via Chocolatey

```powershell
choco install dotnet-sdk -y
```

### Option B – Download manually

Download the **.NET 8 SDK** installer for Windows x64 from:
https://dotnet.microsoft.com/en-us/download/dotnet/8.0

1. Run the installer (`dotnet-sdk-8.x.xxx-win-x64.exe`).
2. Verify:

```powershell
dotnet --version
# Expected: 8.0.x

dotnet --list-sdks
# Expected: 8.0.x [C:\Program Files\dotnet\sdk]
```

---

## 8. Docker Desktop

Docker Desktop is needed to run the PostgreSQL database used by the web application, as well as integration tests that rely on **Testcontainers**.

**Download:** https://www.docker.com/products/docker-desktop/

1. Run the installer (`Docker Desktop Installer.exe`).
2. When prompted, choose **"Use WSL 2 instead of Hyper-V"** (recommended on Windows 10/11).
3. After installation, start Docker Desktop from the Start Menu.
4. Sign in with a Docker account (free) if prompted.
5. Verify:

```powershell
docker --version
# Expected: Docker version 27.x.x, build ...

docker compose version
# Expected: Docker Compose version v2.x.x
```

> **WSL 2 requirement:** Docker Desktop with WSL 2 requires Windows 10 version 2004 (Build 19041) or later. Enable WSL 2 first if needed:
>
> ```powershell
> wsl --install
> wsl --set-default-version 2
> ```

---

## 9. IDE / Editors

### Visual Studio 2022 (WPF desktop app)

Required to develop, build, and debug the WPF desktop application.

**Download:** https://visualstudio.microsoft.com/vs/

1. Run the installer and select the **".NET desktop development"** workload.
2. Ensure **.NET 8.0 runtime** is checked under *Individual components* → *SDKs, libraries, and frameworks*.
3. Finish the installation (it may take several minutes).

### IntelliJ IDEA (Spring Boot backend)

Recommended IDE for developing the Java Spring Boot backend.

**Download Community Edition (free):** https://www.jetbrains.com/idea/download/

Or install via Chocolatey:

```powershell
choco install intellijidea-community -y
```

After opening the project, IntelliJ will auto-detect the Maven `pom.xml` and import dependencies.

### Visual Studio Code (Angular frontend)

Recommended editor for the Angular frontend.

**Download:** https://code.visualstudio.com/

Or install via Chocolatey:

```powershell
choco install vscode -y
```

**Recommended VS Code extensions for Angular development:**

| Extension | Publisher | Purpose |
|---|---|---|
| Angular Language Service | Angular | IntelliSense for Angular templates |
| ESLint | Microsoft | Lint TypeScript/JavaScript |
| Prettier - Code formatter | Prettier | Code formatting |
| EditorConfig for VS Code | EditorConfig | Respect `.editorconfig` settings |

Install all at once from a terminal inside VS Code:

```powershell
code --install-extension angular.ng-template
code --install-extension dbaeumer.vscode-eslint
code --install-extension esbenp.prettier-vscode
code --install-extension editorconfig.editorconfig
```

---

## 10. Verify Your Installation

Open a **new** PowerShell window (so all PATH changes take effect) and run:

```powershell
git --version
choco --version
java -version
mvn -version
node --version
npm --version
ng version
dotnet --version
docker --version
docker compose version
```

All commands should return version numbers without errors. A successful output looks like:

```
git version 2.47.x.windows.x
2.4.x
openjdk version "21.x.x" ...
Apache Maven 3.9.x ...
v22.x.x
10.x.x
Angular CLI: 19.x.x
8.0.x
Docker version 27.x.x, build ...
Docker Compose version v2.x.x
```

---

## Quick Reference: All Tools via Chocolatey

If you want to install everything in one shot after Chocolatey is set up:

```powershell
choco install git microsoft-openjdk21 maven nodejs-lts dotnet-sdk vscode intellijidea-community -y
refreshenv
npm install -g @angular/cli@19
```

Docker Desktop must be downloaded manually from https://www.docker.com/products/docker-desktop/ as it requires a GUI-based installer with licence acceptance.
