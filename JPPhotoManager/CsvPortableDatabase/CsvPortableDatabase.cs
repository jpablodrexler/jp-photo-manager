using System.Data;
using System.IO;
using System.Text;

namespace CsvDb
{
    public class CsvPortableDatabase
    {
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

        public DataTable GetDataTableFromCsv(string csv, string separator)
        {
            DataTable table = new DataTable();

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
    }
}
