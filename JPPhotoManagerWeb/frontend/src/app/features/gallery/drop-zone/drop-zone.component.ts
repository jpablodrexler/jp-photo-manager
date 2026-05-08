import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnInit,
  Output,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { HttpEventType } from '@angular/common/http';
import { AssetService } from '../../../core/services/asset.service';

const ACCEPTED_EXTENSIONS = new Set(['jpg', 'jpeg', 'png', 'gif', 'bmp', 'tiff', 'tif', 'webp']);

interface UploadItem {
  file: File;
  progress: number;
  status: 'pending' | 'uploading' | 'done' | 'error';
}

@Component({
  selector: 'app-drop-zone',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  templateUrl: './drop-zone.component.html',
  styleUrl: './drop-zone.component.scss',
})
export class DropZoneComponent {
  @Input() folderPath!: string;
  @Output() uploadComplete = new EventEmitter<void>();

  @ViewChild('fileInput') private fileInput!: ElementRef<HTMLInputElement>;

  isDragging = false;
  uploadQueue: UploadItem[] = [];

  constructor(private assetService: AssetService) {}

  @HostListener('dragover', ['$event'])
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = true;
  }

  @HostListener('dragleave')
  onDragLeave(): void {
    this.isDragging = false;
  }

  @HostListener('drop', ['$event'])
  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
    if (event.dataTransfer?.files) {
      this.onFilesSelected(event.dataTransfer.files);
    }
  }

  onFilesSelected(fileList: FileList | null): void {
    if (!fileList) return;
    const accepted: File[] = [];
    for (let i = 0; i < fileList.length; i++) {
      const file = fileList[i];
      const ext = file.name.includes('.')
        ? file.name.substring(file.name.lastIndexOf('.') + 1).toLowerCase()
        : '';
      if (ACCEPTED_EXTENSIONS.has(ext)) {
        accepted.push(file);
      }
    }
    for (const file of accepted) {
      this.uploadQueue.push({ file, progress: 0, status: 'pending' });
    }
    if (accepted.length > 0) {
      this.processQueue();
    }
  }

  processQueue(): void {
    const pending = this.uploadQueue.filter(item => item.status === 'pending');
    if (pending.length === 0) {
      this.uploadComplete.emit();
      return;
    }
    const item = pending[0];
    item.status = 'uploading';
    this.assetService.uploadAsset(this.folderPath, item.file).subscribe({
      next: event => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          item.progress = Math.round((event.loaded / event.total) * 100);
        } else if (event.type === HttpEventType.Response) {
          item.progress = 100;
          item.status = 'done';
          this.processQueue();
        }
      },
      error: () => {
        item.status = 'error';
        this.processQueue();
      },
    });
  }

  triggerFileInput(): void {
    this.fileInput.nativeElement.click();
  }
}
