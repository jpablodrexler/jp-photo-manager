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
* Export images to shared folders in the local network

Soon will allow to:
* Add your own metadata to the images
* Search images

For the whole roadmap for the application, please take a look at the issues in this repository.

## Run the application
Open the solution file `JPPhotoManager/JPPhotoManager.sln` and run the `JPPhotoManager/JPPhotoManager.UI/JPPhotoManager.UI.csproj` project.

## Installation instructions
* Download the zip file with the compiled application files (`publish.zip` or `jp-photo-manager-{version}.zip`) for the latest release.
* Unzip the content of the zip file to a new folder.
* Run `JPPhotoManager.UI.exe`.
* The application saves the catalog files in the following folder: `C:\Users\{username}\AppData\Local\JPPhotoManager`.

## Technologies used
* [.NET 6.0](https://dotnet.microsoft.com/)
* [Windows Presentation Foundation](https://docs.microsoft.com/en-us/dotnet/framework/wpf/)
* [xUnit](https://xunit.net/)
* [Moq framework for .NET](https://github.com/moq/moq4)
* [Fluent Assertions](https://fluentassertions.com/)
* [log4net](https://logging.apache.org/log4net/)
