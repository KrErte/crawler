import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Job, JobPage, JobFilters } from '../models/job.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class JobService {
  private readonly API = `${environment.apiUrl}/api/jobs`;

  constructor(private http: HttpClient) {}

  getJobs(params: {
    search?: string; company?: string; source?: string;
    workplaceType?: string; jobType?: string;
    skills?: string[];
    salaryMin?: number; salaryMax?: number;
    sortBy?: string; sortDir?: string;
    page?: number; size?: number;
  }): Observable<JobPage> {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        if (Array.isArray(value)) {
          value.forEach(v => httpParams = httpParams.append(key, v));
        } else {
          httpParams = httpParams.set(key, String(value));
        }
      }
    });
    return this.http.get<JobPage>(this.API, { params: httpParams });
  }

  getJob(id: number): Observable<Job> {
    return this.http.get<Job>(`${this.API}/${id}`);
  }

  getFilters(): Observable<JobFilters> {
    return this.http.get<JobFilters>(`${this.API}/filters`);
  }

  getSuggestions(query: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.API}/suggest`, { params: { q: query } });
  }
}
