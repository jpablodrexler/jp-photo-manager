# JPPhotoManager

![JPPhotoManager](JPPhotoManager/Images/AppIcon.png)

[![Build Status](https://dev.azure.com/jpablodrexler/jp-photo-manager/_apis/build/status/jpablodrexler.jp-photo-manager?branchName=master)](https://dev.azure.com/jpablodrexler/jp-photo-manager/_build/latest?definitionId=10&branchName=master)

## Features
JPPhotoManager is a desktop application that allows:
* Visualization of image galleries
* Change Windows wallpaper
* Find duplicates
* Copy/move images
* Import images from game screenshots folder

Soon will allow to:
* Add your own metadata to the images
* Search images

For the whole roadmap for the application, please take a look at the [Trello board](https://trello.com/b/7OlQJdBw/jp-photo-manager)

## Run the application
Open the solution file `JPPhotoManager/JPPhotoManager.sln` and run the `JPPhotoManager/JPPhotoManager/JPPhotoManager.UI.csproj` project.

## Installation instructions
* Download the zip file with the compiled application files (`jp-photo-manager-{version}.zip`) for the latest release.
* Unzip the content of the zip file to a new folder.
* Run `JPPhotoManager.UI.exe`.
* The application saves the catalog files in the following folder: `C:\Users\{username}\AppData\Local\JPPhotoManager`.

## Technologies used
* [.NET Core 3.1](https://dotnet.microsoft.com/)
* [Windows Presentation Foundation](https://docs.microsoft.com/en-us/dotnet/framework/wpf/)
* [xUnit](https://xunit.net/)
* [Moq framework for .NET](https://github.com/moq/moq4)
* [log4net](https://logging.apache.org/log4net/)
