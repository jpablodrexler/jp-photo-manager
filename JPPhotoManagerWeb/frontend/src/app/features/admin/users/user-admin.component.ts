import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { UserAdminService } from '../../../core/services/user-admin.service';
import { UserAdmin } from '../../../core/models/user-admin.model';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-user-admin',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatTableModule, MatButtonModule,
            MatFormFieldModule, MatInputModule, MatIconModule, MatCardModule, MatDialogModule],
  templateUrl: './user-admin.component.html',
  styleUrl: './user-admin.component.scss'
})
export class UserAdminComponent implements OnInit {
  users: UserAdmin[] = [];
  displayedColumns = ['username', 'createdAt', 'actions'];
  errorMessage: string | null = null;
  showAddForm = false;
  editingPasswordId: string | null = null;

  addForm = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  passwordForm = this.fb.nonNullable.group({
    password: ['', Validators.required]
  });

  constructor(
    private userAdminService: UserAdminService,
    private fb: FormBuilder,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.userAdminService.getUsers().subscribe({
      next: users => (this.users = users),
      error: () => (this.errorMessage = 'Failed to load users.')
    });
  }

  submitAdd(): void {
    if (this.addForm.invalid) return;
    const { username, password } = this.addForm.getRawValue();
    this.userAdminService.createUser(username, password).subscribe({
      next: user => {
        this.users = [...this.users, user];
        this.addForm.reset();
        this.showAddForm = false;
        this.errorMessage = null;
      },
      error: () => (this.errorMessage = 'Failed to create user. Username may already exist.')
    });
  }

  startEditPassword(id: string): void {
    this.editingPasswordId = id;
    this.passwordForm.reset();
  }

  submitPasswordChange(id: string): void {
    if (this.passwordForm.invalid) return;
    const { password } = this.passwordForm.getRawValue();
    this.userAdminService.updatePassword(id, password).subscribe({
      next: () => {
        this.editingPasswordId = null;
        this.errorMessage = null;
      },
      error: () => (this.errorMessage = 'Failed to update password.')
    });
  }

  deleteUser(id: string): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete user', message: 'Delete this user?' }
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.userAdminService.deleteUser(id).subscribe({
        next: () => {
          this.users = this.users.filter(u => u.id !== id);
          this.errorMessage = null;
        },
        error: () => (this.errorMessage = 'Failed to delete user.')
      });
    });
  }
}
