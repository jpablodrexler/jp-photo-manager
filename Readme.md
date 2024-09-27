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
