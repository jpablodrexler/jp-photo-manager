using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace JPPhotoManager.Domain
{
    public class PaginatedData<T>
    {
        public T[] Items { get; set; }
        public int PageIndex { get; set; }
        public int TotalCount { get; set; }
    }
}
