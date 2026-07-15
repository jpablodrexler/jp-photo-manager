import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnDestroy,
  Output,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { HttpEventType } from '@angular/common/http';
import { AssetService } from '../../../core/services/asset.service';
import { UploadAssetResponse } from '../../../core/models/asset.model';
import { UploadItem } from '../../../core/models/upload-item.model';

const ACCEPTED_EXTENSIONS = new Set(['jpg', 'jpeg', 'png', 'gif', 'bmp', 'tiff', 'tif', 'webp']);

@Component({
  selector: 'app-drop-zone',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  templateUrl: './drop-zone.component.html',
  styleUrl: './drop-zone.component.scss',
})
export class DropZoneComponent implements OnDestroy {
  @Input() folderPath!: string;
  @Output() uploadComplete = new EventEmitter<void>();

  @ViewChild('fileInput') private fileInput!: ElementRef<HTMLInputElement>;

  isDragging = false;
  uploadQueue: UploadItem[] = [];

  constructor(private assetService: AssetService) {}

  ngOnDestroy(): void {
    // Close any still-open observe streams (files whose processing hadn't finished when the
    // component was destroyed, e.g. the user navigated away from the gallery mid-upload) so the
    // browser connection and the server-side KafkaProgressRegistry emitter entry aren't leaked.
    for (const item of this.uploadQueue) {
      item.eventSource?.close();
    }
  }

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
      // The multipart POST queue is drained, but files still being processed asynchronously
      // (kafka-async-upload) may not have reached a terminal state yet; checkAllComplete()
      // only emits uploadComplete once every item is done/error.
      this.checkAllComplete();
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
          const body = event.body as UploadAssetResponse | null;
          if (body?.assetId != null) {
            item.status = 'processing';
            this.observeProcessing(item, body.assetId);
          } else {
            // Defensive fallback: the backend always returns an assetId on 202, but if it
            // somehow didn't, don't leave the row stuck forever waiting on an SSE stream we
            // have no assetId to open.
            item.status = 'done';
          }
          this.processQueue();
        }
      },
      error: () => {
        item.status = 'error';
        this.processQueue();
      },
    });
  }

  private observeProcessing(item: UploadItem, assetId: number): void {
    const eventSource = this.assetService.observeUpload(assetId);
    item.eventSource = eventSource;

    eventSource.addEventListener('done', () => {
      item.status = 'done';
      eventSource.close();
      this.checkAllComplete();
    });

    eventSource.addEventListener('failed', () => {
      item.status = 'error';
      eventSource.close();
      this.checkAllComplete();
    });

    eventSource.onerror = () => {
      // A dropped SSE connection is a UX-only concern (design.md risk #1): the placeholder asset
      // row is the durable source of truth and a subsequent gallery refresh reflects the true
      // state regardless, so don't leave the row stuck on "Processing..." forever.
      item.status = 'done';
      eventSource.close();
      this.checkAllComplete();
    };
  }

  private checkAllComplete(): void {
    const stillActive = this.uploadQueue.some(
      item => item.status === 'pending' || item.status === 'uploading' || item.status === 'processing'
    );
    if (!stillActive) {
      this.uploadComplete.emit();
    }
  }

  triggerFileInput(): void {
    this.fileInput.nativeElement.click();
  }
}
