# JPPhotoManager

![JPPhotoManager](JPPhotoManager/Images/AppIcon.png)

[![Test](https://github.com/jpablodrexler/jp-photo-manager/actions/workflows/test.yml/badge.svg)](https://github.com/jpablodrexler/jp-photo-manager/actions/workflows/test.yml)
[![Release](https://github.com/jpablodrexler/jp-photo-manager/actions/workflows/release.yaml/badge.svg)](https://github.com/jpablodrexler/jp-photo-manager/actions/workflows/release.yaml)

## Features
JPPhotoManager is a desktop application that allows:
* Visualization of image galleries
* Change Windows wallpaper
* Find duplicates
* Copy/move images
* Import images from local folders (such as game screenshots folders)
* Import images from shared folders in the local network
* Export images to local folders
* Export images to shared folders in the local network
* Delete images in local or shared folders that are not present in source folder

## Run the application
Open the solution file `JPPhotoManager/JPPhotoManager.sln` and run the `JPPhotoManager/JPPhotoManager.UI/JPPhotoManager.UI.csproj` project.

## Installation instructions
* Download the zip file with the compiled application files (`publish.zip` or `jp-photo-manager-{version}.zip`) for the latest release.
* Unzip the content of the zip file to a new folder.
* Run `JPPhotoManager.UI.exe`.
* The application saves the catalog files in the following folder: `C:\Users\{username}\AppData\Local\JPPhotoManager`.

## Technologies used
* [.NET 8.0](https://dotnet.microsoft.com/)
* [Windows Presentation Foundation](https://docs.microsoft.com/en-us/dotnet/framework/wpf/)
* [Entity Framework Core](https://github.com/dotnet/efcore)
* [SQLite](https://www.sqlite.org/index.html)
* [xUnit](https://xunit.net/)
* [Moq framework for .NET](https://github.com/moq/moq4)
* [Fluent Assertions](https://fluentassertions.com/)
* [log4net](https://logging.apache.org/log4net/)
* [Octokit.net](https://octokitnet.readthedocs.io/en/latest/)
* [coverlet](https://github.com/coverlet-coverage/coverlet)
* [ReportGenerator](https://github.com/danielpalme/ReportGenerator)

---

## Web Application

JPPhotoManager Web is a browser-based rewrite of the desktop application, built with a **Java 21 + Spring Boot 3** REST API backend and an **Angular 19** single-page application frontend.

### Features
* Visualization of image galleries (thumbnail grid and full-size viewer)
* Find duplicates
* Copy/move images between folders
* Sync images between directory pairs
* Convert PNG images to JPEG
* Streaming real-time progress for long-running operations via Server-Sent Events

### Run the web application

**Backend** (requires Java 21 and Maven 3.9+):
```bash
cd JPPhotoManagerWeb/backend
mvn spring-boot:run
```
The API starts at `http://localhost:8080`.

**Frontend** (requires Node.js 22):
```bash
cd JPPhotoManagerWeb/frontend
npm install
npm start
```
Open `http://localhost:4200` in your browser. The dev server automatically proxies `/api` requests to the backend.

### Technologies used (web application)
* [Java 21](https://openjdk.org/)
* [Spring Boot 3](https://spring.io/projects/spring-boot)
* [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
* [SQLite](https://www.sqlite.org/index.html)
* [Flyway](https://flywaydb.org/)
* [Lombok](https://projectlombok.org/)
* [MapStruct](https://mapstruct.org/)
* [Apache Commons Imaging](https://commons.apache.org/proper/commons-imaging/)
* [Angular 19](https://angular.dev/)
* [Angular Material](https://material.angular.io/)
* [Cypress](https://www.cypress.io/)
* [JUnit 5](https://junit.org/junit5/)
* [Mockito](https://site.mockito.org/)
* [AssertJ](https://assertj.github.io/doc/)
