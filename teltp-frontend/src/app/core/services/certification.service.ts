import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiService } from './api.service';
import {
  CertificateResponse, IssueCertificateRequest, VerificationResult,
} from '../models/certification.model';

@Injectable({ providedIn: 'root' })
export class CertificationService {
  private readonly api = inject(ApiService);
  private readonly http = inject(HttpClient);

  issue(req: IssueCertificateRequest): Observable<CertificateResponse> {
    return this.api.post<CertificateResponse>('/certification/issue', req);
  }

  forStudent(studentUuid: string): Observable<CertificateResponse[]> {
    return this.api.get<CertificateResponse[]>(`/certification/students/${studentUuid}`);
  }

  verify(code: string): Observable<VerificationResult> {
    return this.api.get<VerificationResult>(`/certification/verify/${code}`);
  }

  /** The download endpoint returns raw PDF bytes (not an ApiResponse), so fetch as a blob.
      The auth interceptor attaches the Bearer token. */
  downloadPdf(uuid: string): Observable<Blob> {
    return this.http.get(`${environment.apiBaseUrl}/certification/${uuid}/download`, {
      responseType: 'blob',
    });
  }
}
