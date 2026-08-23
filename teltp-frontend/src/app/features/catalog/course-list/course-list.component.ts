import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { CatalogService } from '../../../core/services/catalog.service';
import { CourseResponse } from '../../../core/models/catalog.model';
import { HeroComponent } from '../../../layout/hero/hero.component';

@Component({
  selector: 'app-course-list',
  standalone: true,
  imports: [
    RouterLink, MatCardModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatPaginatorModule, HeroComponent,
  ],
  template: `
    <app-hero eyebrow="TIRDO Training Hub" title="Course catalogue"
              subtitle="Industrial, digital, research, environmental and enterprise skills training."
              [showLogo]="true" />
    <div class="page">
      @if (loading()) {
        <div class="center"><mat-spinner diameter="36" /></div>
      } @else if (courses().length === 0) {
        <div class="surface-card empty">
          <mat-icon>menu_book</mat-icon>
          <p class="muted">No published courses yet. Published courses will appear here.</p>
        </div>
      } @else {
        <div class="card-grid">
          @for (c of courses(); track c.uuid) {
            <mat-card class="course-card surface-card">
              <div class="row gap">
                <span class="chip">{{ label(c.deliveryMode) }}</span>
                @if (c.durationHours) { <span class="chip accent">{{ c.durationHours }}h</span> }
              </div>
              <h3 class="title">{{ c.title }}</h3>
              <p class="muted desc">{{ c.description || 'No description provided.' }}</p>
              <div class="row foot">
                <span class="muted ref">{{ c.referenceNumber }}</span>
                <span class="spacer"></span>
                <a mat-button color="primary" [routerLink]="['/catalog', c.uuid]">View</a>
              </div>
            </mat-card>
          }
        </div>

        <mat-paginator [length]="total()" [pageSize]="size" [pageIndex]="page()"
                       [pageSizeOptions]="[12, 24, 48]" (page)="onPage($event)" />
      }
    </div>
  `,
  styles: [`
    .center { display: flex; justify-content: center; padding: 60px; }
    .empty { text-align: center; padding: 48px; }
    .empty mat-icon { font-size: 40px; height: 40px; width: 40px; color: var(--teltp-muted); }
    .course-card { display: flex; flex-direction: column; gap: 10px; }
    .gap { gap: 6px; }
    .title { margin: 4px 0 0; font-size: 1.2rem; }
    .desc { display: -webkit-box; -webkit-line-clamp: 3; line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
    .foot { margin-top: auto; }
    .ref { font-size: 0.75rem; font-variant-numeric: tabular-nums; }
    mat-paginator { margin-top: 20px; background: transparent; }
  `],
})
export class CourseListComponent {
  private readonly catalog = inject(CatalogService);

  readonly loading = signal(true);
  readonly courses = signal<CourseResponse[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly size = 12;

  constructor() { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.catalog.publishedCourses(this.page(), this.size).subscribe({
      next: (p) => { this.courses.set(p.content); this.total.set(p.totalElements); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onPage(e: PageEvent): void { this.page.set(e.pageIndex); this.load(); }

  label(mode: string): string {
    return { ONLINE: 'Online', IN_PERSON: 'In person', HYBRID: 'Hybrid' }[mode] ?? mode;
  }
}
