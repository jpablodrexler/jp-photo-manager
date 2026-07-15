import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
  ViewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Asset, CropAssetRequest } from '../../../core/models/asset.model';
import { SocialMediaFormatDef, SOCIAL_MEDIA_FORMATS } from '../../../core/models/social-media-format.model';
import { AssetService } from '../../../core/services/asset.service';

interface CropBox {
  x: number;
  y: number;
  width: number;
  height: number;
}

type DragMode = 'move' | 'resize';
type CornerName = 'TL' | 'TR' | 'BL' | 'BR';

@Component({
  selector: 'app-social-media-crop',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatSelectModule],
  templateUrl: './social-media-crop.component.html',
  styleUrl: './social-media-crop.component.scss',
})
export class SocialMediaCropComponent implements AfterViewInit, OnDestroy {
  @Input({ required: true }) asset!: Asset;
  @Output() cancelled = new EventEmitter<void>();

  @ViewChild('cropCanvas') canvasRef!: ElementRef<HTMLCanvasElement>;

  readonly formats = SOCIAL_MEDIA_FORMATS;
  selectedFormat: SocialMediaFormatDef = SOCIAL_MEDIA_FORMATS[0];
  isSaving = false;

  private readonly img = new Image();
  private cropBox: CropBox = { x: 0, y: 0, width: 0, height: 0 };
  private dragMode: DragMode | null = null;
  private activeCorner: CornerName | null = null;
  private dragStartX = 0;
  private dragStartY = 0;
  private cropBoxAtDragStart: CropBox = { x: 0, y: 0, width: 0, height: 0 };

  constructor(
    private readonly assetService: AssetService,
    private readonly snackBar: MatSnackBar,
  ) {}

  ngAfterViewInit(): void {
    this.img.crossOrigin = 'use-credentials';
    this.img.onload = () => this.initCanvas();
    this.img.src = this.asset.imageUrl;
  }

  ngOnDestroy(): void {
    this.img.onload = null;
    this.img.src = '';
  }

  onFormatChange(): void {
    this.initCropBox();
    this.redraw();
  }

  onMouseDown(event: MouseEvent): void {
    const { mouseX, mouseY } = this.getCanvasCoords(event);
    const hit = this.getHitArea(mouseX, mouseY);
    if (!hit) return;
    this.dragMode = hit.mode;
    this.activeCorner = hit.corner ?? null;
    this.dragStartX = mouseX;
    this.dragStartY = mouseY;
    this.cropBoxAtDragStart = { ...this.cropBox };
    event.preventDefault();
  }

  onMouseMove(event: MouseEvent): void {
    const { mouseX, mouseY } = this.getCanvasCoords(event);

    if (!this.dragMode) {
      this.updateCursor(mouseX, mouseY);
      return;
    }

    if (this.dragMode === 'move') {
      const canvas = this.canvasRef.nativeElement;
      const dx = mouseX - this.dragStartX;
      const dy = mouseY - this.dragStartY;
      const { width: w, height: h } = this.cropBoxAtDragStart;
      this.cropBox = {
        x: Math.max(0, Math.min(canvas.width - w, this.cropBoxAtDragStart.x + dx)),
        y: Math.max(0, Math.min(canvas.height - h, this.cropBoxAtDragStart.y + dy)),
        width: w,
        height: h,
      };
    } else if (this.activeCorner) {
      this.resizeFromCorner(mouseX);
    }

    this.redraw();
  }

  onMouseUp(): void {
    this.dragMode = null;
    this.activeCorner = null;
  }

  saveAndDownload(): void {
    const canvas = this.canvasRef.nativeElement;
    const scaleX = this.img.naturalWidth / canvas.width;
    const scaleY = this.img.naturalHeight / canvas.height;

    const request: CropAssetRequest = {
      formatKey: this.selectedFormat.key,
      x: Math.round(this.cropBox.x * scaleX),
      y: Math.round(this.cropBox.y * scaleY),
      width: Math.round(this.cropBox.width * scaleX),
      height: Math.round(this.cropBox.height * scaleY),
    };

    this.isSaving = true;
    this.assetService.cropAsset(this.asset.assetId, request).subscribe({
      next: (newAsset) => {
        this.isSaving = false;
        window.open('/api/assets/' + newAsset.assetId + '/image', '_blank');
        this.cancelled.emit();
      },
      error: () => {
        this.isSaving = false;
        this.snackBar.open('Failed to save crop', 'Dismiss', { duration: 4000 });
      },
    });
  }

  private initCanvas(): void {
    const canvas = this.canvasRef.nativeElement;
    const maxW = Math.min(window.innerWidth * 0.85, 900);
    const maxH = Math.min(window.innerHeight * 0.70, 650);
    const imgAspect = this.img.naturalWidth / this.img.naturalHeight;
    const containerAspect = maxW / maxH;

    if (imgAspect > containerAspect) {
      canvas.width = Math.round(maxW);
      canvas.height = Math.round(maxW / imgAspect);
    } else {
      canvas.height = Math.round(maxH);
      canvas.width = Math.round(maxH * imgAspect);
    }

    this.initCropBox();
    this.redraw();
  }

  private initCropBox(): void {
    const canvas = this.canvasRef.nativeElement;
    const ratio = this.selectedFormat.targetWidth / this.selectedFormat.targetHeight;
    const cw = canvas.width;
    const ch = canvas.height;

    let fitW: number, fitH: number;
    if (ratio > cw / ch) {
      fitW = cw;
      fitH = cw / ratio;
    } else {
      fitH = ch;
      fitW = ch * ratio;
    }

    this.cropBox = {
      x: Math.round((cw - fitW) / 2),
      y: Math.round((ch - fitH) / 2),
      width: Math.round(fitW),
      height: Math.round(fitH),
    };
  }

  private resizeFromCorner(mouseX: number): void {
    const canvas = this.canvasRef.nativeElement;
    const cw = canvas.width;
    const ch = canvas.height;
    const ratio = this.selectedFormat.targetWidth / this.selectedFormat.targetHeight;
    const anchor = this.getAnchorPoint();

    let newW = Math.abs(mouseX - anchor.x);
    let newH = newW / ratio;

    // Clamp to canvas bounds from anchor's perspective
    const leftEdge = this.activeCorner === 'TL' || this.activeCorner === 'BL';
    const topEdge = this.activeCorner === 'TL' || this.activeCorner === 'TR';

    if (leftEdge && anchor.x - newW < 0) { newW = anchor.x; newH = newW / ratio; }
    if (!leftEdge && anchor.x + newW > cw) { newW = cw - anchor.x; newH = newW / ratio; }
    if (topEdge && anchor.y - newH < 0) { newH = anchor.y; newW = newH * ratio; }
    if (!topEdge && anchor.y + newH > ch) { newH = ch - anchor.y; newW = newH * ratio; }

    newW = Math.max(20, newW);
    newH = Math.max(20 / ratio, newH);

    const newX = leftEdge ? anchor.x - newW : anchor.x;
    const newY = topEdge ? anchor.y - newH : anchor.y;

    this.cropBox = {
      x: Math.max(0, newX),
      y: Math.max(0, newY),
      width: Math.round(newW),
      height: Math.round(newH),
    };
  }

  private getAnchorPoint(): { x: number; y: number } {
    const { x, y, width: w, height: h } = this.cropBoxAtDragStart;
    switch (this.activeCorner) {
      case 'TL': return { x: x + w, y: y + h };
      case 'TR': return { x: x,     y: y + h };
      case 'BL': return { x: x + w, y: y };
      case 'BR': return { x: x,     y: y };
      default:   return { x: 0,     y: 0 };
    }
  }

  private getHitArea(mouseX: number, mouseY: number): { mode: DragMode; corner?: CornerName } | null {
    const { x, y, width: w, height: h } = this.cropBox;
    const hs = 14;

    if (mouseX >= x         && mouseX <= x + hs     && mouseY >= y         && mouseY <= y + hs)     return { mode: 'resize', corner: 'TL' };
    if (mouseX >= x + w - hs && mouseX <= x + w      && mouseY >= y         && mouseY <= y + hs)     return { mode: 'resize', corner: 'TR' };
    if (mouseX >= x         && mouseX <= x + hs     && mouseY >= y + h - hs && mouseY <= y + h)     return { mode: 'resize', corner: 'BL' };
    if (mouseX >= x + w - hs && mouseX <= x + w      && mouseY >= y + h - hs && mouseY <= y + h)     return { mode: 'resize', corner: 'BR' };
    if (mouseX > x           && mouseX < x + w       && mouseY > y           && mouseY < y + h)      return { mode: 'move' };

    return null;
  }

  private getCanvasCoords(event: MouseEvent): { mouseX: number; mouseY: number } {
    const rect = this.canvasRef.nativeElement.getBoundingClientRect();
    return { mouseX: event.clientX - rect.left, mouseY: event.clientY - rect.top };
  }

  private updateCursor(mouseX: number, mouseY: number): void {
    const canvas = this.canvasRef.nativeElement;
    const hit = this.getHitArea(mouseX, mouseY);
    if (hit?.mode === 'resize') {
      const corner = hit.corner;
      canvas.style.cursor = (corner === 'TL' || corner === 'BR') ? 'nw-resize' : 'ne-resize';
    } else if (hit?.mode === 'move') {
      canvas.style.cursor = 'move';
    } else {
      canvas.style.cursor = 'crosshair';
    }
  }

  private redraw(): void {
    const canvas = this.canvasRef.nativeElement;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const { x, y, width: w, height: h } = this.cropBox;
    const cw = canvas.width;
    const ch = canvas.height;

    ctx.drawImage(this.img, 0, 0, cw, ch);

    ctx.fillStyle = 'rgba(0, 0, 0, 0.55)';
    ctx.fillRect(0, 0, cw, y);
    ctx.fillRect(0, y + h, cw, ch - y - h);
    ctx.fillRect(0, y, x, h);
    ctx.fillRect(x + w, y, cw - x - w, h);

    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 2;
    ctx.strokeRect(x + 1, y + 1, w - 2, h - 2);

    ctx.strokeStyle = 'rgba(255, 255, 255, 0.35)';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(x + w / 3, y);     ctx.lineTo(x + w / 3, y + h);
    ctx.moveTo(x + 2 * w / 3, y); ctx.lineTo(x + 2 * w / 3, y + h);
    ctx.moveTo(x, y + h / 3);     ctx.lineTo(x + w, y + h / 3);
    ctx.moveTo(x, y + 2 * h / 3); ctx.lineTo(x + w, y + 2 * h / 3);
    ctx.stroke();

    if (this.selectedFormat.isCircle) {
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.9)';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(x + w / 2, y + h / 2, Math.min(w, h) / 2 - 2, 0, 2 * Math.PI);
      ctx.stroke();
    }

    const hs = 10;
    ctx.fillStyle = '#ffffff';
    const corners: Array<[number, number]> = [
      [x, y],
      [x + w - hs, y],
      [x, y + h - hs],
      [x + w - hs, y + h - hs],
    ];
    corners.forEach(([hx, hy]) => ctx.fillRect(hx, hy, hs, hs));
  }
}
