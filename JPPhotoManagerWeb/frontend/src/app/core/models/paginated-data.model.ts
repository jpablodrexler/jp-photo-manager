export interface PaginatedData<T> {
  items: T[];
  pageIndex: number;
  totalPages: number;
  totalItems: number;
}
