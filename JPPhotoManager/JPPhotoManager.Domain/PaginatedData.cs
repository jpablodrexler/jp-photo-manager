namespace JPPhotoManager.Domain
{
    public class PaginatedData<T>
    {
        public T[] Items { get; set; }
        public int PageIndex { get; set; }
        public int TotalCount { get; set; }
    }
}
