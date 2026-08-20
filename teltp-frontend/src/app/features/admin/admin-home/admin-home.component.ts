import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

interface AdminTile { label: string; icon: string; link?: string; ready: boolean; }

@Component({
  selector: 'app-admin-home',
  standalone: true,
  imports: [RouterLink, MatCardModule, MatIconModule],
  template: `
    <div class="page">
      <h1 class="page-title">Administration</h1>
      <p class="page-subtitle">Manage the platform. Course management is wired end-to-end; other modules follow the same pattern.</p>

      <div class="card-grid">
        @for (t of tiles; track t.label) {
          @if (t.ready && t.link) {
            <a [routerLink]="t.link" class="tile surface-card ready">
              <mat-icon>{{ t.icon }}</mat-icon>
              <span class="label">{{ t.label }}</span>
            </a>
          } @else {
            <div class="tile surface-card pending">
              <mat-icon>{{ t.icon }}</mat-icon>
              <span class="label">{{ t.label }}</span>
              <span class="chip accent">Planned</span>
            </div>
          }
        }
      </div>
    </div>
  `,
  styles: [`
    .tile { display: flex; flex-direction: column; align-items: flex-start; gap: 10px; text-decoration: none; color: inherit; min-height: 120px; }
    .tile mat-icon { font-size: 30px; height: 30px; width: 30px; color: var(--teltp-brand); }
    .tile .label { font-weight: 600; font-size: 1.05rem; }
    .tile.ready:hover { border-color: var(--teltp-brand); }
    .tile.pending { opacity: 0.7; }
  `],
})
export class AdminHomeComponent {
  readonly tiles: AdminTile[] = [
    { label: 'Courses', icon: 'menu_book', link: '/admin/courses', ready: true },
    { label: 'Assessment Grading', icon: 'quiz', link: '/admin/grading', ready: true },
    { label: 'Enrolments & Cohorts', icon: 'groups', ready: false },
    { label: 'Certificates', icon: 'workspace_premium', link: '/admin/certificates', ready: true },
    { label: 'Schedule & Webinars', icon: 'event', ready: false },
    { label: 'Billing & Invoices', icon: 'receipt_long', ready: false },
    { label: 'Organizations', icon: 'apartment', ready: false },
    { label: 'Reporting', icon: 'insights', ready: false },
  ];
}
