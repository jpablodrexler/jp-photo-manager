using System.Data;

namespace CsvPortableDatabase
{
    public interface IDatabase
    {
        string DataDirectory { get; }
        string Separator { get; }
        Diagnostics Diagnostics { get; }
        void Initialize(string dataDirectory, string separator);
        DataTable ReadDataTable(string tableName);
        void WriteDataTable(DataTable dataTable);
        string GetBlobsDirectory(string dataDirectory);
        string GetCsvFromDataTable(DataTable table, string separator);
        DataTable GetDataTableFromCsv(string csv, string separator, string tableName);
        string GetTablesDirectory(string dataDirectory);
        void InitializeDirectory(string dataDirectory);
        string ResolveBlobFilePath(string dataDirectory, string thumbnailsFileName);
        string ResolveTableFilePath(string dataDirectory, string entityName);
    }
}
