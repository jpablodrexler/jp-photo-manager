using System.Data;
using System.IO;
using System.Text;

namespace CsvPortableDatabase
{
    public class Database : IDatabase
    {
        private const string DATA_FILE_FORMAT = "{0}.db";

        public string DataDirectory { get; private set; }
        public string Separator { get; private set; }
        public string LastReadFilePath { get; private set; }
        public string LastReadFileRaw { get; private set; }

        public void Initialize(string dataDirectory, string separator)
        {
            this.DataDirectory = dataDirectory;
            this.Separator = separator;
            InitializeDirectory(dataDirectory);
        }

        public DataTable ReadDataTable(string tableName)
        {
            DataTable dataTable = null;
            string dataFilePath = ResolveTableFilePath(this.DataDirectory, tableName);
            this.LastReadFilePath = dataFilePath;
            
            if (File.Exists(dataFilePath))
            {
                string csv = File.ReadAllText(dataFilePath);
                LastReadFileRaw = csv;
                dataTable = GetDataTableFromCsv(csv, this.Separator, tableName);
            }

            return dataTable;
        }

        public void WriteDataTable(DataTable dataTable)
        {
            string csv = GetCsvFromDataTable(dataTable, this.Separator);
            string dataFilePath = ResolveTableFilePath(this.DataDirectory, dataTable.TableName);
            File.WriteAllText(dataFilePath, csv);
        }

        public string GetCsvFromDataTable(DataTable table, string separator)
        {
            StringBuilder builder = new StringBuilder();
            string[] headers = new string[table.Columns.Count];

            for (int i = 0; i < table.Columns.Count; i++)
            {
                headers[i] = table.Columns[i].ColumnName;
            }

            builder.AppendLine(string.Join(separator, headers));

            for (int i = 0; i < table.Rows.Count; i++)
            {
                DataRow row = table.Rows[i];
                string line = string.Join(separator, row.ItemArray);
                builder.AppendLine(line);
            }

            return builder.ToString();
        }

        public DataTable GetDataTableFromCsv(string csv, string separator, string tableName)
        {
            DataTable table = new DataTable(tableName);

            using (StringReader reader = new StringReader(csv))
            {
                string line = reader.ReadLine();
                string[] headers = line.Split(separator);
                bool hasRecord;

                foreach (string header in headers)
                {
                    table.Columns.Add(header);
                }

                do
                {
                    line = reader.ReadLine();
                    hasRecord = !string.IsNullOrEmpty(line);

                    if (hasRecord)
                    {
                        string[] fields = line.Split(separator);
                        table.Rows.Add(fields);
                    }
                }
                while (hasRecord);

                table.AcceptChanges();
            }

            return table;
        }

        public void InitializeDirectory(string dataDirectory)
        {
            Directory.CreateDirectory(dataDirectory);
            Directory.CreateDirectory(GetTablesDirectory(dataDirectory));
            Directory.CreateDirectory(GetBlobsDirectory(dataDirectory));
        }

        public string GetTablesDirectory(string dataDirectory)
        {
            return Path.Combine(dataDirectory, "Tables");
        }

        public string GetBlobsDirectory(string dataDirectory)
        {
            return Path.Combine(dataDirectory, "Blobs");
        }

        public string ResolveTableFilePath(string dataDirectory, string entityName)
        {
            dataDirectory = !string.IsNullOrEmpty(dataDirectory) ? dataDirectory : string.Empty;
            string fileName = string.Format(DATA_FILE_FORMAT, entityName).ToLower();
            return Path.Combine(GetTablesDirectory(dataDirectory), fileName);
        }

        public string ResolveBlobFilePath(string dataDirectory, string thumbnailsFileName)
        {
            return Path.Combine(GetBlobsDirectory(dataDirectory), thumbnailsFileName);
        }
    }
}
