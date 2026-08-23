import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HeroComponent } from '../../../layout/hero/hero.component';
import { ReportingService } from '../../../core/services/reporting.service';
import {
  CompletionDashboard, PlatformKpis, RevenueDashboard, TrainerDashboard,
} from '../../../core/models/reporting.model';

interface Kpi { label: string; value: string; icon: string; }

@Component({
  selector: 'app-reporting-dashboard',
  standalone: true,
  imports: [RouterLink, DecimalPipe, MatCardModule, MatIconModule, MatProgressSpinnerModule, HeroComponent],
  template: `
    <app-hero eyebrow="TIRDO Training Hub" title="Reports & analytics"
              subtitle="Platform performance and confirmed revenue." [showLogo]="true" />
    <div class="page">
      @if (loading()) {
        <div class="center"><mat-spinner diameter="36" /></div>
      } @else {
        <div class="kpi-grid">
          @for (k of kpis(); track k.label) {
            <mat-card class="surface-card kpi">
              <span class="ic"><mat-icon>{{ k.icon }}</mat-icon></span>
              <span class="value">{{ k.value }}</span>
              <span class="label muted">{{ k.label }}</span>
            </mat-card>
          }
        </div>

        @if (revenue(); as rev) {
          <h2 class="section-title">Revenue by channel</h2>
          <mat-card class="surface-card table-card">
            <div class="rrow head">
              <span>Channel</span><span class="num">Transactions</span><span class="num">Amount</span>
            </div>
            @if (rev.byChannel.length === 0) {
              <div class="rrow"><span class="muted">No confirmed revenue yet.</span></div>
            } @else {
              @for (c of rev.byChannel; track c.channel) {
                <div class="rrow">
                  <span>{{ prettyChannel(c.channel) }}</span>
                  <span class="num">{{ c.transactions | number }}</span>
                  <span class="num">{{ rev.currency }} {{ c.amount | number:'1.0-0' }}</span>
                </div>
              }
              <div class="rrow total">
                <span>Total confirmed</span><span class="num"></span>
                <span class="num">{{ rev.currency }} {{ rev.totalConfirmed | number:'1.0-0' }}</span>
              </div>
            }
          </mat-card>
        }

        @if (completion(); as comp) {
          <h2 class="section-title">Course completion</h2>
          <mat-card class="surface-card table-card">
            <div class="rrow c head"><span>Course</span><span class="num">Enrolled</span><span class="num">Completed</span><span class="num">Rate</span></div>
            @if (comp.rows.length === 0) {
              <div class="rrow"><span class="muted">No published courses with enrolments yet.</span></div>
            } @else {
              @for (r of comp.rows; track r.courseUuid) {
                <div class="rrow c">
                  <span>{{ r.courseTitle }}</span>
                  <span class="num">{{ r.enrolled | number }}</span>
                  <span class="num">{{ r.completed | number }}</span>
                  <span class="num"><span class="rate" [style.--p.%]="r.completionRate">{{ r.completionRate }}%</span></span>
                </div>
              }
            }
          </mat-card>
        }

        @if (trainer(); as tr) {
          <h2 class="section-title">Trainer activity</h2>
          <mat-card class="surface-card table-card">
            <div class="rrow t head"><span>Instructor</span><span class="num">Courses authored</span><span class="num">Learners taught</span></div>
            @if (tr.rows.length === 0) {
              <div class="rrow"><span class="muted">No courses have an assigned instructor yet.</span></div>
            } @else {
              @for (r of tr.rows; track r.instructorUuid) {
                <div class="rrow t">
                  <span>{{ r.instructorName }}</span>
                  <span class="num">{{ r.coursesAuthored | number }}</span>
                  <span class="num">{{ r.learnersTaught | number }}</span>
                </div>
              }
            }
          </mat-card>
        }

        <p class="muted foot"><a routerLink="/dashboard">Back to dashboard</a></p>
      }
    </div>
  `,
  styles: [`
    .center { display: flex; justify-content: center; padding: 60px; }
    .kpi-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 14px; }
    .kpi { display: flex; flex-direction: column; gap: 4px; padding: 18px; }
    .kpi .ic { width: 40px; height: 40px; border-radius: 11px; display: inline-flex; align-items: center; justify-content: center;
      background: rgba(26,77,152,0.10); color: var(--teltp-brand); margin-bottom: 6px; }
    .kpi .value { font-family: 'Spectral', Georgia, serif; font-size: 1.9rem; font-weight: 700; line-height: 1; }
    .kpi .label { font-size: 0.85rem; }
    .section-title { font-family: 'Spectral', Georgia, serif; font-size: 1.3rem; margin: 28px 0 14px; }
    .table-card { padding: 6px 10px; }
    .rrow { display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 12px; padding: 12px 8px; border-bottom: 1px solid var(--teltp-line); }
    .rrow:last-child { border-bottom: none; }
    .rrow.head { font-size: 0.78rem; text-transform: uppercase; letter-spacing: 0.04em; color: var(--teltp-muted); font-weight: 700; }
    .rrow.total { font-weight: 700; }
    .num { text-align: right; font-variant-numeric: tabular-nums; }
    .rrow.c { grid-template-columns: 2fr 1fr 1fr 1fr; }
    .rrow.t { grid-template-columns: 2fr 1fr 1fr; }
    .mono { font-variant-numeric: tabular-nums; }
    .rate { display: inline-block; min-width: 3.2em; padding: 2px 8px; border-radius: 999px; font-weight: 600; font-size: 0.85rem;
      background: linear-gradient(90deg, rgba(26,77,152,0.16) var(--p, 0%), transparent var(--p, 0%)); }
    .foot { margin-top: 24px; }
  `],
})
export class ReportingDashboardComponent {
  private readonly reporting = inject(ReportingService);
  readonly loading = signal(true);
  readonly kpis = signal<Kpi[]>([]);
  readonly revenue = signal<RevenueDashboard | null>(null);
  readonly completion = signal<CompletionDashboard | null>(null);
  readonly trainer = signal<TrainerDashboard | null>(null);

  constructor() {
    this.reporting.kpis().subscribe({
      next: (k) => { this.kpis.set(this.toKpis(k)); this.tryDone(); },
      error: () => this.tryDone(),
    });
    this.reporting.revenue().subscribe({
      next: (r) => { this.revenue.set(r); this.tryDone(); },
      error: () => this.tryDone(),
    });
    this.reporting.completion().subscribe({
      next: (c) => { this.completion.set(c); this.tryDone(); },
      error: () => this.tryDone(),
    });
    this.reporting.trainer().subscribe({
      next: (t) => { this.trainer.set(t); this.tryDone(); },
      error: () => this.tryDone(),
    });
  }

  private pending = 4;
  private tryDone(): void { if (--this.pending <= 0) this.loading.set(false); }

  private toKpis(k: PlatformKpis): Kpi[] {
    return [
      { label: 'Active learners', value: `${k.activeLearners}`, icon: 'group' },
      { label: 'Published courses', value: `${k.publishedCourses}`, icon: 'menu_book' },
      { label: 'Certificates issued', value: `${k.certificatesIssued}`, icon: 'workspace_premium' },
      { label: 'Corporate clients', value: `${k.corporateClients}`, icon: 'domain' },
      { label: 'Confirmed revenue', value: `${k.currency} ${Math.round(k.confirmedRevenue).toLocaleString()}`, icon: 'payments' },
    ];
  }

  prettyChannel(c: string): string {
    return c.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (x) => x.toUpperCase());
  }
}
