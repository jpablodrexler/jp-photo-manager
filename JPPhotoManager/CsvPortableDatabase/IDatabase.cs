using System.Data;

namespace CsvPortableDatabase
{
    public interface IDatabase
    {
        string GetBlobsDirectory(string dataDirectory);
        string GetCsvFromDataTable(DataTable table, string separator);
        DataTable GetDataTableFromCsv(string csv, string separator, string tableName);
        string GetTablesDirectory(string dataDirectory);
        void InitializeDirectory(string dataDirectory);
        string ResolveBlobFilePath(string dataDirectory, string thumbnailsFileName);
        string ResolveTableFilePath(string dataDirectory, string entityName);
    }
}
