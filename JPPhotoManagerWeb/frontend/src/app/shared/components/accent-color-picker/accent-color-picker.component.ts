import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-accent-color-picker',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './accent-color-picker.component.html',
  styleUrl: './accent-color-picker.component.scss',
})
export class AccentColorPickerComponent {
  @Input() accentColor: string | null = null;
  @Output() accentColorSelected = new EventEmitter<string>();

  readonly PALETTE = [
    { label: 'Forest Green', value: '#2e7d32' },
    { label: 'Ocean Blue',   value: '#1565c0' },
    { label: 'Deep Purple',  value: '#6a1b9a' },
    { label: 'Teal',         value: '#00695c' },
    { label: 'Rust Orange',  value: '#bf360c' },
    { label: 'Slate Grey',   value: '#37474f' },
    { label: 'Crimson Red',  value: '#b71c1c' },
    { label: 'Indigo',       value: '#283593' },
  ];

  select(color: string): void {
    this.accentColorSelected.emit(color);
  }
}
