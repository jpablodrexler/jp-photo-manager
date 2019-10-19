using Microsoft.Extensions.Configuration;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Moq;
using System;
using System.Collections.Generic;
using System.Text;

namespace JPPhotoManager.Test
{
    [TestClass]
    public class ConfigurationMockExtensionsTest
    {
        [TestMethod]
        public void ConfigurationMockTest()
        {
            Mock<IConfigurationRoot> configurationMock = new Mock<IConfigurationRoot>();
            configurationMock
                .MockGetValue("appsettings:InitialDirectory", "dataDirectory1")
                .MockGetValue("appsettings:ApplicationDataDirectory", "dataDirectory2")
                .MockGetValue("appsettings:CatalogBatchSize", "100");

            IConfigurationRoot configuration = configurationMock.Object;
            Assert.AreEqual("dataDirectory1", configuration.GetValue<string>("appsettings:InitialDirectory"));
            Assert.AreEqual("dataDirectory2", configuration.GetValue<string>("appsettings:ApplicationDataDirectory"));
            Assert.AreEqual("100", configuration.GetValue<string>("appsettings:CatalogBatchSize"));
        }
    }
}
