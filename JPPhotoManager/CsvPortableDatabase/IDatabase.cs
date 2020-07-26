using System.Data;

namespace CsvPortableDatabase
{
    public interface IDatabase
    {
        string GetCsvFromDataTable(DataTable table, string separator);
        DataTable GetDataTableFromCsv(string csv, string separator, string tableName);
    }
}
