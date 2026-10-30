import { Component, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { ApiService } from '../../core/services/api.service';

interface Notice { uuid: string; title: string; body: string; read: boolean; }
interface Page<T> { content: T[]; }

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [MatCardModule, MatButtonModule],
  template: `
    <div class="page">
      <h1 class="page-title">Notices</h1>
      @if (items().length === 0) { <p class="muted">No notices yet.</p> }
      @for (n of items(); track n.uuid) {
        <mat-card class="surface-card" style="margin-bottom: 12px; padding: 16px;">
          <strong>{{ n.title }}</strong>
          <p>{{ n.body }}</p>
          @if (!n.read) {
            <button mat-button (click)="mark(n)">Mark read</button>
          }
        </mat-card>
      }
    </div>
  `,
})
export class NotificationsComponent {
  private readonly api = inject(ApiService);
  readonly items = signal<Notice[]>([]);

  constructor() {
    this.api.get<Page<Notice>>('/notifications').subscribe((p) => this.items.set(p.content ?? []));
  }

  mark(n: Notice): void {
    this.api.post<void>(`/notifications/${n.uuid}/read`, {}).subscribe(() => {
      this.items.update((list) => list.map((x) => x.uuid === n.uuid ? { ...x, read: true } : x));
    });
  }
}
