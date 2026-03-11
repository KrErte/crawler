import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Job } from '../models/job.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class RecommendationService {
  private readonly API = `${environment.apiUrl}/api/recommendations`;

  constructor(private http: HttpClient) {}

  getRecommendations(limit: number = 10): Observable<Job[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<Job[]>(this.API, { params });
  }
}
