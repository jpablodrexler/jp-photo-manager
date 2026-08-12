import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CatalogState } from '../../../core/models/catalog-notification.model';

@Component({
  selector: 'app-catalog-progress-footer',
  standalone: true,
  imports: [DatePipe, MatIconModule, MatButtonModule, MatProgressBarModule, MatTooltipModule],
  templateUrl: './catalog-progress-footer.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './catalog-progress-footer.component.scss',
})
export class CatalogProgressFooterComponent {
  @Input() state: CatalogState = 'idle';
  @Input() percentCompleted = 0;
  @Input() currentStatusText = '';
  @Input() lastCompletedAt: Date | null = null;

  @Output() reconnect = new EventEmitter<void>();
}
