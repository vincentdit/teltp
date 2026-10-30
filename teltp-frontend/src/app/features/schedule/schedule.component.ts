import { Component, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { ApiService } from '../../core/services/api.service';

interface EventItem {
  uuid: string; title: string; type: string; startsAt: string; joinUrl?: string;
}
interface Page<T> { content: T[]; }

@Component({
  selector: 'app-schedule',
  standalone: true,
  imports: [MatCardModule, MatButtonModule],
  template: `
    <div class="page">
      <h1 class="page-title">Schedule</h1>
      <div style="display:flex; gap:8px; margin-bottom:16px;">
        <button mat-stroked-button (click)="load('WEBINAR')">Webinars</button>
        <button mat-stroked-button (click)="load('TRAINING_SESSION')">Sessions</button>
        <button mat-stroked-button (click)="load('WORKSHOP')">Workshops</button>
      </div>
      @if (items().length === 0) { <p class="muted">Nothing scheduled for this type.</p> }
      @for (e of items(); track e.uuid) {
        <mat-card class="surface-card" style="margin-bottom: 12px; padding: 16px;">
          <strong>{{ e.title }}</strong>
          <p class="muted">{{ e.type }} · {{ e.startsAt }}</p>
          @if (e.joinUrl) { <p><a [href]="e.joinUrl" target="_blank" rel="noopener">Join</a></p> }
          <button mat-button (click)="register(e)">Register</button>
        </mat-card>
      }
    </div>
  `,
})
export class ScheduleComponent {
  private readonly api = inject(ApiService);
  readonly items = signal<EventItem[]>([]);

  constructor() { this.load('WEBINAR'); }

  load(type: string): void {
    this.api.get<Page<EventItem>>('/schedule/events', { type }).subscribe((p) => this.items.set(p.content ?? []));
  }

  register(e: EventItem): void {
    this.api.post('/schedule/register', { eventUuid: e.uuid }).subscribe();
  }
}
