using JPPhotoManager.Domain;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Xunit;

namespace JPPhotoManager.Tests.Unit
{
    public class BatchRenameTest
    {
        [Theory]
        [InlineData("MyImage.jpg", "", 1, "")]
        [InlineData("MyImage.jpg", ".jpg", 1, "")]
        [InlineData("MyImage.jpg", "#.jpg", 1, "1.jpg")]
        [InlineData("MyImage.jpg", "##.jpg", 1, "01.jpg")]
        [InlineData("MyImage.jpg", "###.jpg", 1, "001.jpg")]
        [InlineData("MyImage.jpg", "####.jpg", 1, "0001.jpg")]
        [InlineData("MyImage.jpg", "#####.jpg", 1, "00001.jpg")]
        [InlineData("MyImage.jpg", "######.jpg", 1, "000001.jpg")]
        [InlineData("MyImage.jpg", "##_Image.jpg", 1, "01_Image.jpg")]
        [InlineData("MyImage.jpg", "Image_##.jpg", 1, "Image_01.jpg")]
        [InlineData("MyImage.jpg", "My_##_Image.jpg", 1, "My_01_Image.jpg")]
        [InlineData("MyImage.png", ".png", 1, "")]
        [InlineData("MyImage.png", "#.png", 1, "1.png")]
        [InlineData("MyImage.png", "##.png", 1, "01.png")]
        [InlineData("MyImage.png", "###.png", 1, "001.png")]
        [InlineData("MyImage.png", "####.png", 1, "0001.png")]
        [InlineData("MyImage.png", "#####.png", 1, "00001.png")]
        [InlineData("MyImage.png", "######.png", 1, "000001.png")]
        [InlineData("MyImage.JPG", ".JPG", 1, "")]
        [InlineData("MyImage.JPG", "#.JPG", 1, "1.JPG")]
        [InlineData("MyImage.JPG", "##.JPG", 1, "01.JPG")]
        [InlineData("MyImage.JPG", "###.JPG", 1, "001.JPG")]
        [InlineData("MyImage.JPG", "####.JPG", 1, "0001.JPG")]
        [InlineData("MyImage.JPG", "#####.JPG", 1, "00001.JPG")]
        [InlineData("MyImage.JPG", "######.JPG", 1, "000001.JPG")]
        public void ComputeNewName_ReturnNewName(string existingName, string batchFormat, int ordinal, string expectedNewName)
        {
            Asset asset = new()
            {
                FileName = existingName
            };

            string newName = asset.ComputeNewName(batchFormat, ordinal);
            Assert.Equal(expectedNewName, newName);
        }
    }
}
