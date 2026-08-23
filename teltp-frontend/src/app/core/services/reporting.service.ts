import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import {
  CompletionDashboard, PlatformKpis, RevenueDashboard, TrainerDashboard,
} from '../models/reporting.model';

@Injectable({ providedIn: 'root' })
export class ReportingService {
  private readonly api = inject(ApiService);

  kpis(): Observable<PlatformKpis> {
    return this.api.get<PlatformKpis>('/reporting/kpis');
  }

  revenue(): Observable<RevenueDashboard> {
    return this.api.get<RevenueDashboard>('/reporting/revenue');
  }

  completion(): Observable<CompletionDashboard> {
    return this.api.get<CompletionDashboard>('/reporting/completion');
  }

  trainer(): Observable<TrainerDashboard> {
    return this.api.get<TrainerDashboard>('/reporting/trainer');
  }
}
