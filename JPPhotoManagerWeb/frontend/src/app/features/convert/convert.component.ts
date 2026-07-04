import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatListModule } from '@angular/material/list';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ConvertService } from '../../core/services/convert.service';
import { ConvertAssetsDirectoriesDefinition, ConvertAssetsResult } from '../../core/models/convert-config.model';
import { AuthService } from '../../core/services/auth.service';

type ProcessStep = 'configure' | 'running' | 'results';

@Component({
  selector: 'app-convert',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatCheckboxModule,
    MatCardModule,
    MatProgressBarModule,
    MatListModule,
    MatSnackBarModule
  ],
  templateUrl: './convert.component.html',
  styleUrl: './convert.component.scss'
})
export class ConvertComponent implements OnInit {

  step: ProcessStep = 'configure';
  definitions: ConvertAssetsDirectoriesDefinition[] = [];
  statusMessages: string[] = [];
  results: ConvertAssetsResult[] = [];
  running = false;

  displayedColumns = ['sourceDirectory', 'destinationDirectory', 'includeSubFolders', 'actions'];

  constructor(
    private convertService: ConvertService,
    private snackBar: MatSnackBar,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.convertService.getConfiguration().subscribe({
      next: defs => this.definitions = defs,
      error: () => this.snackBar.open('Failed to load configuration', 'Dismiss', { duration: 3000 })
    });
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
    this.convertService.setConfiguration(this.definitions).subscribe({
      next: () => this.runConvert(),
      error: () => this.snackBar.open('Failed to save configuration', 'Dismiss', { duration: 3000 })
    });
  }

  private runConvert(): void {
    this.step = 'running';
    this.running = true;
    this.statusMessages = [];
    this.results = [];

    const eventSource = this.convertService.run();

    eventSource.addEventListener('status', (event: MessageEvent) => {
      this.statusMessages.push(event.data);
    });

    eventSource.addEventListener('results', (event: MessageEvent) => {
      this.results = JSON.parse(event.data);
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
