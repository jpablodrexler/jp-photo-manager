#Asset Manager

Asset Manager is a desktop application that allows:
* Visualization of image galleries
* Copy/move images
* Find duplicates

Soon will allow to:
* Import images from game screenshots folder
* Add your own metadata to the images
* Search images

## Run the application
Open the solution file `AssetManager/AssetManager.sln` and run the `AssetManager/AssetManager/AssetManager.csproj` project.

## Installation instructions
* Unzip the content of the zip file to a new folder.
* Run `AssetManager.exe`.
* The application builds the catalog files in the following folder: `C:\Users\{username}\AppData\Local\AssetManager`.

## Technologies used
* Windows Presentation Foundation
* [Simple Injector](https://simpleinjector.org/index.html)
* [Newtonsoft Json.NET](https://www.newtonsoft.com/json)
* [Moq framework for .NET](https://github.com/moq/moq4)
* [log4net](https://logging.apache.org/log4net/)
