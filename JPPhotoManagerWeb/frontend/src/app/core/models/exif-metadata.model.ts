export interface ExifMetadata {
  cameraMake: string | null;
  cameraModel: string | null;
  lensModel: string | null;
  exposureTime: string | null;
  fNumber: number | null;
  isoSpeed: number | null;
  focalLength: number | null;
  dateTaken: string | null;
  widthPixels: number | null;
  heightPixels: number | null;
  gpsLatitude: number | null;
  gpsLongitude: number | null;
}
