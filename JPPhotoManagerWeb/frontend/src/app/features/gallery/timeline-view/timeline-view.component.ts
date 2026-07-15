import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ThumbnailComponent } from '../../../shared/components/thumbnail/thumbnail.component';
import { TimelineGroup } from '../../../core/models/timeline-group.model';
import { Asset } from '../../../core/models/asset.model';

@Component({
  selector: 'app-timeline-view',
  standalone: true,
  imports: [ThumbnailComponent],
  templateUrl: './timeline-view.component.html',
  styleUrl: './timeline-view.component.scss',
})
export class TimelineViewComponent {
  @Input() groups: TimelineGroup[] = [];
  @Output() thumbnailClick = new EventEmitter<Asset>();

  currentMonth(group: TimelineGroup, index: number): string | null {
    const month = group.localDate.substring(0, 7);
    if (index === 0) return month;
    const prevMonth = this.groups[index - 1].localDate.substring(0, 7);
    return month !== prevMonth ? month : null;
  }

  formatMonthHeader(localDate: string): string {
    const [year, month] = localDate.split('-').map(Number);
    return new Date(year, month - 1, 1).toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  }

  formatDayHeader(localDate: string): string {
    const [year, month, day] = localDate.split('-').map(Number);
    return new Date(year, month - 1, day).toLocaleDateString('en-US', { weekday: 'long', day: 'numeric' });
  }
}
