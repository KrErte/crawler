import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MatchResult, JobMatchScore } from '../models/match.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class MatchService {
  private readonly API = `${environment.apiUrl}/api/match`;

  constructor(private http: HttpClient) {}

  matchJobs(file: File, topN: number = 20): Observable<MatchResult[]> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<MatchResult[]>(`${this.API}?topN=${topN}`, formData);
  }

  matchFromProfile(topN: number = 20): Observable<MatchResult[]> {
    return this.http.post<MatchResult[]>(`${this.API}/profile?topN=${topN}`, null);
  }

  getMatchScores(jobIds: number[]): Observable<JobMatchScore[]> {
    return this.http.post<JobMatchScore[]>(`${this.API}/scores`, jobIds);
  }
}
