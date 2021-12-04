using Microsoft.Extensions.Configuration;
using Moq;

namespace JPPhotoManager.Tests
{
    public static class ConfigurationMockExtensions
    {
        public static Mock<IConfigurationRoot> MockGetValue(this Mock<IConfigurationRoot> configurationMock, string key, string value)
        {
            Mock<IConfigurationSection> sectionMock = new();
            sectionMock.SetupGet(s => s.Value).Returns(value);
            configurationMock.Setup(c => c.GetSection(key)).Returns(sectionMock.Object);

            return configurationMock;
        }
    }
}
