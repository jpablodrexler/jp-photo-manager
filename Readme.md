# JPPhotoManager

![JPPhotoManager](JPPhotoManager/Images/AppIcon.png)

[![Build Status](https://dev.azure.com/jpablodrexler/jp-photo-manager/_apis/build/status/jpablodrexler.jp-photo-manager?branchName=master)](https://dev.azure.com/jpablodrexler/jp-photo-manager/_build/latest?definitionId=10&branchName=master)

[![Board Status](https://dev.azure.com/jpablodrexler/aaf58f4b-bfb1-47e5-b4db-e3901cc5fb48/0b3e1f82-4359-4a71-9fa2-191c79553891/_apis/work/boardbadge/f84ef2a8-b659-48c5-816a-cd85c5ae0446)](https://dev.azure.com/jpablodrexler/aaf58f4b-bfb1-47e5-b4db-e3901cc5fb48/_boards/board/t/0b3e1f82-4359-4a71-9fa2-191c79553891/Microsoft.RequirementCategory)

JPPhotoManager is a desktop application that allows:
* Visualization of image galleries
* Change Windows wallpaper
* Find duplicates
* Copy/move images

Soon will allow to:
* Import images from game screenshots folder
* Add your own metadata to the images
* Search images

## Run the application
Open the solution file `JPPhotoManager/JPPhotoManager.sln` and run the `JPPhotoManager/JPPhotoManager/JPPhotoManager.csproj` project.

## Installation instructions
* Unzip the content of the zip file to a new folder.
* Run `JPPhotoManager.exe`.
* The application builds the catalog files in the following folder: `C:\Users\{username}\AppData\Local\JPPhotoManager`.

## Technologies used
* Windows Presentation Foundation
* [Simple Injector](https://simpleinjector.org/index.html)
* [Newtonsoft Json.NET](https://www.newtonsoft.com/json)
* [Moq framework for .NET](https://github.com/moq/moq4)
* [log4net](https://logging.apache.org/log4net/)
