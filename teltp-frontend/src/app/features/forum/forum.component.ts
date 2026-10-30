import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { ApiService } from '../../core/services/api.service';

interface Thread { uuid: string; title: string; body: string; }
interface Page<T> { content: T[]; }

@Component({
  selector: 'app-forum',
  standalone: true,
  imports: [ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <div class="page">
      <h1 class="page-title">Course forum</h1>
      <p class="muted">Paste a course id from the catalogue address to read and post.</p>
      <form [formGroup]="lookup" (ngSubmit)="load()" class="stack" style="max-width: 560px;">
        <mat-form-field appearance="outline">
          <mat-label>Course id</mat-label>
          <input matInput formControlName="courseUuid" />
        </mat-form-field>
        <button mat-stroked-button type="submit">Load threads</button>
      </form>

      @for (t of threads(); track t.uuid) {
        <mat-card class="surface-card" style="margin: 12px 0; padding: 16px;">
          <strong>{{ t.title }}</strong>
          <p>{{ t.body }}</p>
        </mat-card>
      }

      <h2>New thread</h2>
      <form [formGroup]="create" (ngSubmit)="post()" class="stack" style="max-width: 560px;">
        <mat-form-field appearance="outline">
          <mat-label>Title</mat-label>
          <input matInput formControlName="title" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Message</mat-label>
          <textarea matInput formControlName="body" rows="3"></textarea>
        </mat-form-field>
        <button mat-flat-button color="primary" type="submit" [disabled]="create.invalid">Post</button>
      </form>
    </div>
  `,
})
export class ForumComponent {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  readonly threads = signal<Thread[]>([]);
  readonly lookup = this.fb.nonNullable.group({ courseUuid: ['', Validators.required] });
  readonly create = this.fb.nonNullable.group({
    title: ['', Validators.required],
    body: [''],
  });

  load(): void {
    const courseUuid = this.lookup.controls.courseUuid.value;
    this.api.get<Page<Thread>>(`/forum/courses/${courseUuid}/threads`).subscribe((p) => this.threads.set(p.content ?? []));
  }

  post(): void {
    const courseUuid = this.lookup.controls.courseUuid.value;
    this.api.post('/forum/threads', { courseUuid, ...this.create.getRawValue() }).subscribe(() => this.load());
  }
}
