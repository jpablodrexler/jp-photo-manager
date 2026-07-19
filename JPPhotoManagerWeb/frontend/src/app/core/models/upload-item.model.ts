export interface UploadItem {
  file: File;
  progress: number;
  status: 'pending' | 'uploading' | 'processing' | 'done' | 'error';
  eventSource?: EventSource;
}
