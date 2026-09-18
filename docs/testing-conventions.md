# Testing Conventions

Stack: **xUnit** + **Autofac.Extras.Moq** + **FluentAssertions**.

Typical unit test pattern:

```csharp
[Fact]
public void MethodName_Condition_ExpectedResult()
{
    using var mock = AutoMock.GetLoose();
    mock.Mock<ISomeDependency>().Setup(m => m.Method(...)).Returns(...);
    var sut = mock.Container.Resolve<SomeClass>();
    var result = sut.DoSomething();
    result.Should().BeEquivalentTo(expected);
}
```

Integration tests (in `Integration/`) use real (in-memory or temp-file) SQLite databases and exercise the full stack from `Application` down through repositories.

Test data files (images, folders) live under `JPPhotoManager.Tests/TestFiles/`.
