import { Component, Inject, OnDestroy, OnInit, ChangeDetectionStrategy, signal } from '@angular/core';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule, MatChipInputEvent } from '@angular/material/chips';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { Subject, forkJoin, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { TagService } from '../../../core/services/tag.service';
import { BulkTagDialogData } from '../../../core/models/dialog.model';

@Component({
  selector: 'app-bulk-tag-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatAutocompleteModule,
    MatInputModule,
    MatFormFieldModule,
    MatDialogModule,
    MatProgressSpinnerModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './bulk-tag-dialog.component.html',
})
export class BulkTagDialogComponent implements OnInit, OnDestroy {
  tagsToAdd: string[] = [];
  tagsToRemove: string[] = [];
  readonly addSuggestions = signal<string[]>([]);
  readonly removeSuggestions = signal<string[]>([]);
  addInputControl = new FormControl<string>('', { nonNullable: true });
  removeInputControl = new FormControl<string>('', { nonNullable: true });
  readonly separatorKeysCodes = [ENTER, COMMA] as const;
  readonly isSaving = signal(false);

  private readonly destroy$ = new Subject<void>();

  constructor(
    private tagService: TagService,
    private dialogRef: MatDialogRef<BulkTagDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: BulkTagDialogData,
  ) {}

  ngOnInit(): void {
    this.addInputControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(q => {
      if (q && q.length >= 1) {
        this.tagService.searchTags(q).subscribe(tags => {
          this.addSuggestions.set(tags.filter(t => !this.tagsToAdd.includes(t)));
        });
      } else {
        this.addSuggestions.set([]);
      }
    });

    this.removeInputControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(q => {
      if (q && q.length >= 1) {
        this.tagService.searchTags(q).subscribe(tags => {
          this.removeSuggestions.set(tags.filter(t => !this.tagsToRemove.includes(t)));
        });
      } else {
        this.removeSuggestions.set([]);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  addTagToAdd(event: MatChipInputEvent): void {
    const name = (event.value ?? '').trim().toLowerCase();
    event.chipInput.clear();
    this.addInputControl.setValue('', { emitEvent: false });
    if (!name || this.tagsToAdd.includes(name)) return;
    this.tagsToAdd = [...this.tagsToAdd, name];
  }

  addTagToAddFromAutocomplete(event: MatAutocompleteSelectedEvent): void {
    const name = event.option.viewValue.toLowerCase();
    this.addInputControl.setValue('', { emitEvent: false });
    this.addSuggestions.set([]);
    if (!name || this.tagsToAdd.includes(name)) return;
    this.tagsToAdd = [...this.tagsToAdd, name];
  }

  removeTagToAdd(name: string): void {
    this.tagsToAdd = this.tagsToAdd.filter(t => t !== name);
  }

  addTagToRemove(event: MatChipInputEvent): void {
    const name = (event.value ?? '').trim().toLowerCase();
    event.chipInput.clear();
    this.removeInputControl.setValue('', { emitEvent: false });
    if (!name || this.tagsToRemove.includes(name)) return;
    this.tagsToRemove = [...this.tagsToRemove, name];
  }

  addTagToRemoveFromAutocomplete(event: MatAutocompleteSelectedEvent): void {
    const name = event.option.viewValue.toLowerCase();
    this.removeInputControl.setValue('', { emitEvent: false });
    this.removeSuggestions.set([]);
    if (!name || this.tagsToRemove.includes(name)) return;
    this.tagsToRemove = [...this.tagsToRemove, name];
  }

  removeTagToRemove(name: string): void {
    this.tagsToRemove = this.tagsToRemove.filter(t => t !== name);
  }

  confirm(): void {
    if (this.tagsToAdd.length === 0 && this.tagsToRemove.length === 0) {
      this.dialogRef.close(false);
      return;
    }
    this.isSaving.set(true);
    const ids = this.data.assetIds;
    const addCalls = this.tagsToAdd.map(tag => this.tagService.bulkAddTag(ids, tag));
    const removeCalls = this.tagsToRemove.map(tag => this.tagService.bulkRemoveTag(ids, tag));
    forkJoin([...addCalls, ...removeCalls].length > 0 ? [...addCalls, ...removeCalls] : [of(undefined)]).subscribe({
      next: () => this.dialogRef.close(true),
      error: () => {
        this.isSaving.set(false);
      },
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
