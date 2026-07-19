import { Component, OnDestroy, OnInit } from '@angular/core';
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
import { SyncService } from '../../core/services/sync.service';
import { SyncAssetsDirectoriesDefinition, SyncAssetsResult } from '../../core/models/sync-config.model';
import { AuthService } from '../../core/services/auth.service';

type ProcessStep = 'configure' | 'running' | 'results';

@Component({
  selector: 'app-sync',
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
  templateUrl: './sync.component.html',
  styleUrl: './sync.component.scss'
})
export class SyncComponent implements OnInit, OnDestroy {

  step: ProcessStep = 'configure';
  definitions: SyncAssetsDirectoriesDefinition[] = [];
  statusMessages: string[] = [];
  results: SyncAssetsResult[] = [];
  running = false;

  private eventSource?: EventSource;

  displayedColumns = ['sourceDirectory', 'destinationDirectory', 'includeSubFolders', 'deleteAssetsNotInSource', 'actions'];

  constructor(
    private syncService: SyncService,
    private snackBar: MatSnackBar,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.syncService.getConfiguration().subscribe({
      next: defs => this.definitions = defs,
      error: () => this.snackBar.open('Failed to load configuration', 'Dismiss', { duration: 3000 })
    });
  }

  ngOnDestroy(): void {
    this.eventSource?.close();
  }

  addDefinition(): void {
    this.definitions = [...this.definitions, {
      sourceDirectory: '',
      destinationDirectory: '',
      includeSubFolders: false,
      deleteAssetsNotInSource: false,
      order: this.definitions.length
    }];
  }

  removeDefinition(index: number): void {
    this.definitions = this.definitions.filter((_, i) => i !== index);
  }

  moveUp(index: number): void {
    if (index > 0) {
      const updated = [...this.definitions];
      [updated[index - 1], updated[index]] = [updated[index], updated[index - 1]];
      this.definitions = updated;
    }
  }

  moveDown(index: number): void {
    if (index < this.definitions.length - 1) {
      const updated = [...this.definitions];
      [updated[index], updated[index + 1]] = [updated[index + 1], updated[index]];
      this.definitions = updated;
    }
  }

  saveAndRun(): void {
    this.syncService.setConfiguration(this.definitions).subscribe({
      next: () => this.runSync(),
      error: () => this.snackBar.open('Failed to save configuration', 'Dismiss', { duration: 3000 })
    });
  }

  private runSync(): void {
    this.step = 'running';
    this.running = true;
    this.statusMessages = [];
    this.results = [];

    const eventSource = this.syncService.run();
    this.eventSource = eventSource;

    eventSource.addEventListener('status', (event: MessageEvent) => {
      this.statusMessages.push(event.data);
    });

    eventSource.addEventListener('results', (event: MessageEvent) => {
      this.results = JSON.parse(event.data as string) as SyncAssetsResult[];
      this.step = 'results';
      this.running = false;
      eventSource.close();
    });

    eventSource.addEventListener('error', () => {
      this.running = false;
      this.step = 'results';
      eventSource.close();
    });
  }

  backToConfigure(): void {
    this.step = 'configure';
  }
}
