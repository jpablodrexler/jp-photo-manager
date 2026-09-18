# Build and Test Commands

## Build

```bash
dotnet restore JPPhotoManager/JPPhotoManager.sln
dotnet build --no-restore --configuration Release JPPhotoManager/JPPhotoManager.sln
```

## Test

Run all tests:

```bash
dotnet test --no-build --configuration Release --verbosity normal JPPhotoManager/JPPhotoManager.Tests/JPPhotoManager.Tests.csproj
```

Run a single test class (`--filter "ClassName=<Name>"`):

```bash
dotnet test --no-build --configuration Release --verbosity normal JPPhotoManager/JPPhotoManager.Tests/JPPhotoManager.Tests.csproj --filter "ClassName=ApplicationTests"
```

Run a single test method (`--filter "FullyQualifiedName=<FullName>"`):

```bash
dotnet test --no-build --configuration Release --verbosity normal JPPhotoManager/JPPhotoManager.Tests/JPPhotoManager.Tests.csproj --filter "FullyQualifiedName=JPPhotoManager.Tests.Unit.Application.ApplicationTests.MethodName"
```

Run tests with coverage (Windows only; generates `TestResults/index.htm`):

```bash
cd JPPhotoManager && ./test-with-coverage.bat
```
