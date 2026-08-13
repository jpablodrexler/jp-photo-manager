import { ChangeDetectorRef, Component, EventEmitter, HostBinding, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule, MatChipInputEvent } from '@angular/material/chips';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { AssetService } from '../../../core/services/asset.service';
import { TagService } from '../../../core/services/tag.service';
import { Asset } from '../../../core/models/asset.model';
import { ExifMetadata } from '../../../core/models/exif-metadata.model';

@Component({
  selector: 'app-exif-panel',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatExpansionModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatAutocompleteModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './exif-panel.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './exif-panel.component.scss'
})
export class ExifPanelComponent implements OnChanges, OnInit, OnDestroy {
  @Input({ required: true }) asset!: Asset;
  @Input() visible = false;
  @Output() closed = new EventEmitter<void>();

  @HostBinding('style.display')
  get hostDisplay(): string {
    return this.visible ? 'flex' : 'none';
  }

  readonly exif = signal<ExifMetadata | null>(null);
  readonly loading = signal(false);
  filterText = '';
  readonly tagSuggestions = signal<string[]>([]);
  tagInputControl = new FormControl<string>('', { nonNullable: true });
  readonly separatorKeysCodes = [ENTER, COMMA] as const;

  private cache = new Map<number, ExifMetadata | null>();
  private destroy$ = new Subject<void>();
  private readonly cdr = inject(ChangeDetectorRef);

  constructor(
    private assetService: AssetService,
    private tagService: TagService,
  ) {}

  ngOnInit(): void {
    this.tagInputControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(q => {
      if (q && q.length >= 1) {
        this.tagService.searchTags(q).pipe(takeUntil(this.destroy$)).subscribe(tags => {
          this.tagSuggestions.set(tags.filter(t => !(this.asset?.tags ?? []).includes(t)));
        });
      } else {
        this.tagSuggestions.set([]);
      }
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if ('asset' in changes) {
      this.tagSuggestions.set([]);
      this.tagInputControl.setValue('', { emitEvent: false });
    }

    if (this.visible && this.asset?.assetId != null && ('visible' in changes || 'asset' in changes)) {
      const assetId = this.asset.assetId;
      if (this.cache.has(assetId)) {
        this.exif.set(this.cache.get(assetId) ?? null);
        this.loading.set(false);
      } else {
        this.loading.set(true);
        this.exif.set(null);
        this.assetService.getExifMetadata(assetId).subscribe({
          next: (data) => {
            this.cache.set(assetId, data);
            this.exif.set(data);
            this.loading.set(false);
          },
          error: () => {
            this.cache.set(assetId, null);
            this.loading.set(false);
          }
        });
      }
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  addTag(event: MatChipInputEvent): void {
    const name = (event.value ?? '').trim().toLowerCase();
    event.chipInput.clear();
    this.tagInputControl.setValue('', { emitEvent: false });
    if (!name || (this.asset.tags ?? []).includes(name)) return;
    this.asset.tags = [...(this.asset.tags ?? []), name];
    this.tagService.addTag(this.asset.assetId, name).subscribe({
      error: () => {
        this.asset.tags = (this.asset.tags ?? []).filter(t => t !== name);
        this.cdr.markForCheck();
      }
    });
  }

  addTagFromAutocomplete(event: MatAutocompleteSelectedEvent): void {
    const name = event.option.viewValue.toLowerCase();
    this.tagInputControl.setValue('', { emitEvent: false });
    this.tagSuggestions.set([]);
    if (!name || (this.asset.tags ?? []).includes(name)) return;
    this.asset.tags = [...(this.asset.tags ?? []), name];
    this.tagService.addTag(this.asset.assetId, name).subscribe({
      error: () => {
        this.asset.tags = (this.asset.tags ?? []).filter(t => t !== name);
        this.cdr.markForCheck();
      }
    });
  }

  removeTag(name: string): void {
    const previousTags = [...(this.asset.tags ?? [])];
    this.asset.tags = (this.asset.tags ?? []).filter(t => t !== name);
    this.tagService.removeTag(this.asset.assetId, name).subscribe({
      error: () => {
        this.asset.tags = previousTags;
        this.cdr.markForCheck();
      }
    });
  }

  filteredRawExif(): { key: string; value: string }[] {
    return Object.entries(this.exif()?.rawExif ?? {})
      .filter(([k]) => k.toLowerCase().includes(this.filterText.toLowerCase()))
      .map(([key, value]) => ({ key, value }));
  }

  close(): void {
    this.closed.emit();
  }
}
