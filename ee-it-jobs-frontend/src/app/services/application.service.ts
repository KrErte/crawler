import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Application, CreateApplicationRequest, UpdateApplicationRequest } from '../models/application.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApplicationService {
  private readonly API = `${environment.apiUrl}/api/applications`;

  constructor(private http: HttpClient) {}

  getApplications(status?: string): Observable<Application[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<Application[]>(this.API, { params });
  }

  createApplication(req: CreateApplicationRequest): Observable<Application> {
    return this.http.post<Application>(this.API, req);
  }

  updateApplication(id: number, req: UpdateApplicationRequest): Observable<Application> {
    return this.http.put<Application>(`${this.API}/${id}`, req);
  }

  deleteApplication(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/${id}`);
  }

  exportApplications(format: string): Observable<Blob> {
    return this.http.get(`${this.API}/export`, {
      params: { format },
      responseType: 'blob'
    });
  }

  checkExists(jobId: number): Observable<{ exists: boolean }> {
    return this.http.get<{ exists: boolean }>(`${this.API}/check/${jobId}`);
  }
}
