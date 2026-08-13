import { Component, OnDestroy, OnInit, ChangeDetectionStrategy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatListModule } from '@angular/material/list';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ConvertService } from '../../core/services/convert.service';
import { ConvertAssetsDirectoriesDefinition, ConvertAssetsResult } from '../../core/models/convert-config.model';
import { AuthService } from '../../core/services/auth.service';

type ProcessStep = 'configure' | 'running' | 'results';

@Component({
  selector: 'app-convert',
  standalone: true,
  imports: [
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatCheckboxModule,
    MatCardModule,
    MatProgressBarModule,
    MatListModule
  ],
  templateUrl: './convert.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './convert.component.scss'
})
export class ConvertComponent implements OnInit, OnDestroy {

  readonly step = signal<ProcessStep>('configure');
  readonly definitions = signal<ConvertAssetsDirectoriesDefinition[]>([]);
  readonly statusMessages = signal<string[]>([]);
  readonly results = signal<ConvertAssetsResult[]>([]);
  readonly running = signal(false);

  private eventSource?: EventSource;

  displayedColumns = ['sourceDirectory', 'destinationDirectory', 'includeSubFolders', 'actions'];

  constructor(
    private convertService: ConvertService,
    private snackBar: MatSnackBar,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.convertService.getConfiguration().subscribe({
      next: defs => this.definitions.set(defs),
      error: () => this.snackBar.open('Failed to load configuration', 'Dismiss', { duration: 3000 })
    });
  }

  ngOnDestroy(): void {
    this.eventSource?.close();
  }

  addDefinition(): void {
    this.definitions.update(defs => [...defs, {
      sourceDirectory: '',
      destinationDirectory: '',
      includeSubFolders: false,
      deleteAssetsNotInSource: false,
      order: defs.length
    }]);
  }

  removeDefinition(index: number): void {
    this.definitions.update(defs => defs.filter((_, i) => i !== index));
  }

  moveUp(index: number): void {
    if (index > 0) {
      this.definitions.update(defs => {
        const updated = [...defs];
        [updated[index - 1], updated[index]] = [updated[index], updated[index - 1]];
        return updated;
      });
    }
  }

  moveDown(index: number): void {
    this.definitions.update(defs => {
      if (index >= defs.length - 1) return defs;
      const updated = [...defs];
      [updated[index], updated[index + 1]] = [updated[index + 1], updated[index]];
      return updated;
    });
  }

  saveAndRun(): void {
    this.convertService.setConfiguration(this.definitions()).subscribe({
      next: () => this.runConvert(),
      error: () => this.snackBar.open('Failed to save configuration', 'Dismiss', { duration: 3000 })
    });
  }

  private runConvert(): void {
    this.step.set('running');
    this.running.set(true);
    this.statusMessages.set([]);
    this.results.set([]);

    const eventSource = this.convertService.run();
    this.eventSource = eventSource;

    eventSource.addEventListener('status', (event: MessageEvent) => {
      this.statusMessages.update(msgs => [...msgs, event.data]);
    });

    eventSource.addEventListener('results', (event: MessageEvent) => {
      this.results.set(JSON.parse(event.data as string) as ConvertAssetsResult[]);
      this.step.set('results');
      this.running.set(false);
      eventSource.close();
    });

    eventSource.addEventListener('error', () => {
      this.running.set(false);
      this.step.set('results');
      eventSource.close();
    });
  }

  backToConfigure(): void {
    this.step.set('configure');
  }
}
