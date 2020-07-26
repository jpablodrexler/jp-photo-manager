using FluentAssertions;
using System.Data;
using Xunit;

namespace CsvDb.Tests
{
    public class CsvPortableDatabaseTest
    {
        [Fact]
        public void GetCsvFromDataTableTest()
        {
            string expected = "FolderId;FileName;FileSize;ImageRotation;PixelWidth;PixelHeight;ThumbnailPixelWidth;ThumbnailPixelHeight;ThumbnailCreationDateTime;Hash\r\n" +
                "876283c6-780e-4ad5-975c-be63044c087a;20200720175810_3.jpg;363888;Rotate0;1920;1080;200;112;25/07/2020 9:45:47;4e50d5c7f1a64b5d61422382ac822641ad4e5b943aca9ade955f4655f799558bb0ae9c342ee3ead0949b32019b25606bd16988381108f56bb6c6dd673edaa1e4\r\n" +
                "876283c6-780e-4ad5-975c-be63044c087a;20200720175816_3.jpg;343633;Rotate0;1920;1080;200;112;25/07/2020 9:45:47;0af8f118b7d606e5d174643727bd3c0c6028b52c50481585274fd572110b108c7a0d7901227f75a72b44c89335e002a65e8137ff5b238ab1c0bba0505e783124\r\n";

            DataTable table = new DataTable();
            table.Columns.Add("FolderId");
            table.Columns.Add("FileName");
            table.Columns.Add("FileSize");
            table.Columns.Add("ImageRotation");
            table.Columns.Add("PixelWidth");
            table.Columns.Add("PixelHeight");
            table.Columns.Add("ThumbnailPixelWidth");
            table.Columns.Add("ThumbnailPixelHeight");
            table.Columns.Add("ThumbnailCreationDateTime");
            table.Columns.Add("Hash");

            DataRow row = table.NewRow();
            row["FolderId"] = "876283c6-780e-4ad5-975c-be63044c087a";
            row["FileName"] = "20200720175810_3.jpg";
            row["FileSize"] = "363888";
            row["ImageRotation"] = "Rotate0";
            row["PixelWidth"] = "1920";
            row["PixelHeight"] = "1080";
            row["ThumbnailPixelWidth"] = "200";
            row["ThumbnailPixelHeight"] = "112";
            row["ThumbnailCreationDateTime"] = "25/07/2020 9:45:47";
            row["Hash"] = "4e50d5c7f1a64b5d61422382ac822641ad4e5b943aca9ade955f4655f799558bb0ae9c342ee3ead0949b32019b25606bd16988381108f56bb6c6dd673edaa1e4";
            table.Rows.Add(row);

            row = table.NewRow();
            row["FolderId"] = "876283c6-780e-4ad5-975c-be63044c087a";
            row["FileName"] = "20200720175816_3.jpg";
            row["FileSize"] = "343633";
            row["ImageRotation"] = "Rotate0";
            row["PixelWidth"] = "1920";
            row["PixelHeight"] = "1080";
            row["ThumbnailPixelWidth"] = "200";
            row["ThumbnailPixelHeight"] = "112";
            row["ThumbnailCreationDateTime"] = "25/07/2020 9:45:47";
            row["Hash"] = "0af8f118b7d606e5d174643727bd3c0c6028b52c50481585274fd572110b108c7a0d7901227f75a72b44c89335e002a65e8137ff5b238ab1c0bba0505e783124";
            table.Rows.Add(row);

            table.AcceptChanges();

            CsvPortableDatabase portableDatabase = new CsvPortableDatabase();
            string result = portableDatabase.GetCsvFromDataTable(table, ";");

            result.Should().Be(expected);
        }

        [Fact]
        public void GetDataTableFromCsvTest()
        {
            string csv = "FolderId;FileName;FileSize;ImageRotation;PixelWidth;PixelHeight;ThumbnailPixelWidth;ThumbnailPixelHeight;ThumbnailCreationDateTime;Hash\r\n" +
                "876283c6-780e-4ad5-975c-be63044c087a;20200720175810_3.jpg;363888;Rotate0;1920;1080;200;112;25/07/2020 9:45:47;4e50d5c7f1a64b5d61422382ac822641ad4e5b943aca9ade955f4655f799558bb0ae9c342ee3ead0949b32019b25606bd16988381108f56bb6c6dd673edaa1e4\r\n" +
                "876283c6-780e-4ad5-975c-be63044c087a;20200720175816_3.jpg;343633;Rotate0;1920;1080;200;112;25/07/2020 9:45:47;0af8f118b7d606e5d174643727bd3c0c6028b52c50481585274fd572110b108c7a0d7901227f75a72b44c89335e002a65e8137ff5b238ab1c0bba0505e783124\r\n";

            CsvPortableDatabase portableDatabase = new CsvPortableDatabase();
            DataTable table = portableDatabase.GetDataTableFromCsv(csv, ";", "Asset");
            
            table.Columns.Should().HaveCount(10);
            table.Columns[0].ColumnName.Should().Be("FolderId");
            table.Columns[1].ColumnName.Should().Be("FileName");
            table.Columns[2].ColumnName.Should().Be("FileSize");
            table.Columns[3].ColumnName.Should().Be("ImageRotation");
            table.Columns[4].ColumnName.Should().Be("PixelWidth");
            table.Columns[5].ColumnName.Should().Be("PixelHeight");
            table.Columns[6].ColumnName.Should().Be("ThumbnailPixelWidth");
            table.Columns[7].ColumnName.Should().Be("ThumbnailPixelHeight");
            table.Columns[8].ColumnName.Should().Be("ThumbnailCreationDateTime");
            table.Columns[9].ColumnName.Should().Be("Hash");

            table.TableName.Should().Be("Asset");
            table.Rows.Should().HaveCount(2);
            
            table.Rows[0]["FolderId"].Should().Be("876283c6-780e-4ad5-975c-be63044c087a");
            table.Rows[0]["FileName"].Should().Be("20200720175810_3.jpg");
            table.Rows[0]["FileSize"].Should().Be("363888");
            table.Rows[0]["ImageRotation"].Should().Be("Rotate0");
            table.Rows[0]["PixelWidth"].Should().Be("1920");
            table.Rows[0]["PixelHeight"].Should().Be("1080");
            table.Rows[0]["ThumbnailPixelWidth"].Should().Be("200");
            table.Rows[0]["ThumbnailPixelHeight"].Should().Be("112");
            table.Rows[0]["ThumbnailCreationDateTime"].Should().Be("25/07/2020 9:45:47");
            table.Rows[0]["Hash"].Should().Be("4e50d5c7f1a64b5d61422382ac822641ad4e5b943aca9ade955f4655f799558bb0ae9c342ee3ead0949b32019b25606bd16988381108f56bb6c6dd673edaa1e4");

            table.Rows[1]["FolderId"].Should().Be("876283c6-780e-4ad5-975c-be63044c087a");
            table.Rows[1]["FileName"].Should().Be("20200720175816_3.jpg");
            table.Rows[1]["FileSize"].Should().Be("343633");
            table.Rows[1]["ImageRotation"].Should().Be("Rotate0");
            table.Rows[1]["PixelWidth"].Should().Be("1920");
            table.Rows[1]["PixelHeight"].Should().Be("1080");
            table.Rows[1]["ThumbnailPixelWidth"].Should().Be("200");
            table.Rows[1]["ThumbnailPixelHeight"].Should().Be("112");
            table.Rows[1]["ThumbnailCreationDateTime"].Should().Be("25/07/2020 9:45:47");
            table.Rows[1]["Hash"].Should().Be("0af8f118b7d606e5d174643727bd3c0c6028b52c50481585274fd572110b108c7a0d7901227f75a72b44c89335e002a65e8137ff5b238ab1c0bba0505e783124");
        }
    }
}
