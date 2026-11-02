import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../../core/services/api.service';
import { PortalSlide } from '../../../core/models/portal-slide.model';

@Component({
  selector: 'app-portal-slides',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatCheckboxModule, MatIconModule,
  ],
  template: `
    <div class="page">
      <h1 class="page-title">Sign-in notices</h1>
      <p class="page-subtitle">
        These slides move on the sign-in page. Add as many as you need for events, programmes, or announcements.
        Use JPEG, PNG, or WebP images up to 5 MB.
      </p>

      @if (message()) { <p class="notice" role="status">{{ message() }}</p> }
      @if (error()) { <p class="form-error" role="alert">{{ error() }}</p> }

      <form class="surface-card stack" [formGroup]="form" (ngSubmit)="save()">
        <h2>{{ editing() ? 'Edit slide' : 'Add a slide' }}</h2>
        <mat-form-field appearance="outline">
          <mat-label>Title</mat-label>
          <input matInput formControlName="title" maxlength="160" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Message</mat-label>
          <textarea matInput rows="3" formControlName="body" maxlength="500"></textarea>
        </mat-form-field>
        <div class="two">
          <mat-form-field appearance="outline">
            <mat-label>Link (optional)</mat-label>
            <input matInput formControlName="linkUrl" placeholder="/catalog or https://..." />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Order</mat-label>
            <input matInput type="number" formControlName="sortOrder" />
          </mat-form-field>
        </div>
        <label class="file">
          Image
          <input type="file" accept="image/jpeg,image/png,image/webp" (change)="onFile($event)" />
        </label>
        @if (editing()) {
          <mat-checkbox formControlName="removeImage">Remove the current image</mat-checkbox>
        }
        <mat-checkbox formControlName="active">Show on the sign-in page</mat-checkbox>
        <div class="row">
          <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving()">
            {{ saving() ? 'Saving…' : (editing() ? 'Update slide' : 'Add slide') }}
          </button>
          @if (editing()) {
            <button mat-button type="button" (click)="cancel()">Cancel</button>
          }
        </div>
      </form>

      <div class="list">
        @for (slide of slides(); track slide.uuid) {
          <article class="surface-card item">
            @if (slide.hasImage) {
              <img [src]="imageUrl(slide)" [alt]="slide.title" />
            } @else {
              <div class="placeholder">No image</div>
            }
            <div>
              <strong>{{ slide.title }}</strong>
              <p class="muted">{{ slide.body }}</p>
              <p class="muted">Order {{ slide.sortOrder }} · {{ slide.active ? 'Visible' : 'Hidden' }}</p>
            </div>
            <div class="row">
              <button mat-button type="button" (click)="edit(slide)"><mat-icon>edit</mat-icon> Edit</button>
              <button mat-button type="button" (click)="remove(slide)"><mat-icon>delete</mat-icon> Remove</button>
            </div>
          </article>
        } @empty {
          <p class="muted">No slides yet. Add one above.</p>
        }
      </div>
    </div>
  `,
  styles: [`
    h2 { margin: 0; font-size: 1.3rem; }
    .notice, .form-error { padding: 10px 12px; border-radius: 10px; }
    .notice { background: #eef6ee; color: #135c2a; }
    .form-error { background: #fdecec; color: #8a1f1f; }
    .two { display: grid; grid-template-columns: 1fr 140px; gap: 12px; }
    .file { display: flex; flex-direction: column; gap: 6px; font-weight: 600; }
    .list { display: flex; flex-direction: column; gap: 12px; margin-top: 20px; }
    .item { display: grid; grid-template-columns: 160px 1fr auto; gap: 16px; align-items: center; }
    .item img, .placeholder { width: 160px; height: 96px; object-fit: cover; border-radius: 10px; background: #e7e4da; }
    .placeholder { display: grid; place-items: center; color: var(--teltp-muted); font-size: 0.85rem; }
    .item p { margin: 4px 0 0; }
    @media (max-width: 720px) {
      .two, .item { grid-template-columns: 1fr; }
      .item img, .placeholder { width: 100%; }
    }
  `],
})
export class PortalSlidesComponent {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly slides = signal<PortalSlide[]>([]);
  readonly editing = signal<string | null>(null);
  readonly saving = signal(false);
  readonly message = signal('');
  readonly error = signal('');
  private file: File | null = null;

  readonly form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    body: [''],
    linkUrl: [''],
    sortOrder: [1],
    active: [true],
    removeImage: [false],
  });

  constructor() { this.reload(); }

  imageUrl(slide: PortalSlide): string {
    return `/api/portal/slides/${slide.uuid}/image?v=${slide.imageVersion ?? 0}`;
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.file = input.files && input.files.length ? input.files[0] : null;
  }

  edit(slide: PortalSlide): void {
    this.editing.set(slide.uuid);
    this.file = null;
    this.message.set('');
    this.error.set('');
    this.form.setValue({
      title: slide.title,
      body: slide.body ?? '',
      linkUrl: slide.linkUrl ?? '',
      sortOrder: slide.sortOrder,
      active: slide.active,
      removeImage: false,
    });
  }

  cancel(): void {
    this.editing.set(null);
    this.file = null;
    this.form.reset({ title: '', body: '', linkUrl: '', sortOrder: this.slides().length + 1, active: true, removeImage: false });
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.error.set('');
    this.message.set('');
    const raw = this.form.getRawValue();
    const data = new FormData();
    data.set('title', raw.title.trim());
    data.set('body', raw.body.trim());
    data.set('linkUrl', raw.linkUrl.trim());
    data.set('sortOrder', String(raw.sortOrder ?? 0));
    data.set('active', String(raw.active));
    data.set('removeImage', String(raw.removeImage));
    if (this.file) data.set('image', this.file);
    const id = this.editing();
    const request = id
      ? this.api.putForm<PortalSlide>(`/portal/slides/${id}`, data)
      : this.api.postForm<PortalSlide>('/portal/slides', data);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.message.set(id ? 'Slide updated.' : 'Slide added. It will appear on the sign-in page.');
        this.cancel();
        this.reload();
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? 'Could not save the slide.');
      },
    });
  }

  remove(slide: PortalSlide): void {
    if (!confirm(`Remove “${slide.title}”?`)) return;
    this.api.delete<void>(`/portal/slides/${slide.uuid}`).subscribe({
      next: () => { this.message.set('Slide removed.'); this.reload(); },
      error: (err) => this.error.set(err?.error?.message ?? 'Could not remove the slide.'),
    });
  }

  private reload(): void {
    this.api.get<PortalSlide[]>('/portal/slides/manage').subscribe({
      next: (rows) => {
        this.slides.set(rows ?? []);
        if (!this.editing()) this.form.controls.sortOrder.setValue((rows?.length ?? 0) + 1);
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Could not load slides.'),
    });
  }
}
