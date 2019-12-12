using JPPhotoManager.Converters;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.Threading;

namespace JPPhotoManager.Test
{
    [TestClass]
    public class FileSizeConverterTest
    {
        [TestMethod]
        public void GetFormattedFileSizeBytesTest()
        {
            FileSizeConverter converter = new FileSizeConverter();
            string result = (string)converter.Convert(656, typeof(long), null, Thread.CurrentThread.CurrentCulture);
            Assert.AreEqual("656 bytes", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeKilobytesTest1()
        {
            FileSizeConverter converter = new FileSizeConverter();
            string result = (string)converter.Convert(17734, typeof(long), null, Thread.CurrentThread.CurrentCulture);
            Assert.AreEqual(17.3 + " KB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeKilobytesTest2()
        {
            FileSizeConverter converter = new FileSizeConverter();
            string result = (string)converter.Convert(20480, typeof(long), null, Thread.CurrentThread.CurrentCulture);
            Assert.AreEqual(20.ToString("0.0") + " KB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeKilobytesTest3()
        {
            FileSizeConverter converter = new FileSizeConverter();
            string result = (string)converter.Convert(562688, typeof(long), null, Thread.CurrentThread.CurrentCulture);
            Assert.AreEqual(549.5 + " KB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeKilobytesTest4()
        {
            FileSizeConverter converter = new FileSizeConverter();
            string result = (string)converter.Convert(565248, typeof(long), null, Thread.CurrentThread.CurrentCulture);
            Assert.AreEqual(552.ToString("0.0") + " KB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeMegabytesTest1()
        {
            FileSizeConverter converter = new FileSizeConverter();
            string result = (string)converter.Convert(54712102, typeof(long), null, Thread.CurrentThread.CurrentCulture);
            Assert.AreEqual(52.2 + " MB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeMegabytesTest2()
        {
            FileSizeConverter converter = new FileSizeConverter();
            string result = (string)converter.Convert(54956032, typeof(long), null, Thread.CurrentThread.CurrentCulture);
            Assert.AreEqual(52.4 + " MB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeGigabytesTest1()
        {
            FileSizeConverter converter = new FileSizeConverter();
            string result = (string)converter.Convert(1742919342, typeof(long), null, Thread.CurrentThread.CurrentCulture);
            Assert.AreEqual(1.6 + " GB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeGigabytesTest2()
        {
            FileSizeConverter converter = new FileSizeConverter();
            string result = (string)converter.Convert(1753653248, typeof(long), null, Thread.CurrentThread.CurrentCulture);
            Assert.AreEqual(1.6 + " GB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeGigabytesTest3()
        {
            FileSizeConverter converter = new FileSizeConverter();
            string result = (string)converter.Convert(24998490626, typeof(long), null, Thread.CurrentThread.CurrentCulture);
            Assert.AreEqual(23.3 + " GB", result);
        }

        [TestMethod]
        public void GetFormattedFileSizeGigabytesTest4()
        {
            FileSizeConverter converter = new FileSizeConverter();
            string result = (string)converter.Convert(25073561600, typeof(long), null, Thread.CurrentThread.CurrentCulture);
            Assert.AreEqual(23.4 + " GB", result);
        }
    }
}
