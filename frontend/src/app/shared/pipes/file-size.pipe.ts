import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'fileSize', standalone: true })
export class FileSizePipe implements PipeTransform {

  transform(bytes: number): string {
    if (bytes === 0) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return `${(bytes / Math.pow(1024, i)).toFixed(1)} ${units[i]}`;
  }
}
