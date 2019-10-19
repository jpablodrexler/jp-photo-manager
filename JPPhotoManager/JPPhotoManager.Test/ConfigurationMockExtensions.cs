using Microsoft.Extensions.Configuration;
using Moq;
using System;
using System.Collections.Generic;
using System.Text;

namespace JPPhotoManager.Test
{
    public static class ConfigurationMockExtensions
    {
        public static Mock<IConfigurationRoot> MockGetValue(this Mock<IConfigurationRoot> configurationMock, string key, string value)
        {
            Mock<IConfigurationSection> sectionMock = new Mock<IConfigurationSection>();
            sectionMock.SetupGet(s => s.Value).Returns(value);
            configurationMock.Setup(c => c.GetSection(key)).Returns(sectionMock.Object);

            return configurationMock;
        }
    }
}
