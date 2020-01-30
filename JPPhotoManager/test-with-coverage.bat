dotnet test /p:CollectCoverage=true /p:CoverletOutput=TestResults/ /p:CoverletOutputFormat=lcov /p:Exclude="[*Tests*]" /p:ExcludeByAttribute="GeneratedCodeAttribute" /p:ExcludeByAttribute="ExcludeFromCodeCoverageAttribute"

dotnet tool install dotnet-reportgenerator-globaltool --tool-path tools
.\tools\reportgenerator.exe -reports:JPPhotoManager.Tests\TestResults\coverage.info -targetdir:.\TestResults\
start .\TestResults\index.htm
