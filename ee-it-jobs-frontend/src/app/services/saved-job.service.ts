import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Job } from '../models/job.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SavedJobService {
  private readonly API = `${environment.apiUrl}/api/saved-jobs`;
  private _savedIds = new Set<number>();

  constructor(private http: HttpClient) {}

  get savedIds(): Set<number> {
    return this._savedIds;
  }

  getSavedJobs(): Observable<Job[]> {
    return this.http.get<Job[]>(this.API);
  }

  loadSavedJobIds(): Observable<number[]> {
    return this.http.get<number[]>(`${this.API}/ids`).pipe(
      tap(ids => {
        this._savedIds = new Set(ids);
      })
    );
  }

  saveJob(jobId: number): Observable<void> {
    this._savedIds.add(jobId);
    return this.http.post<void>(`${this.API}/${jobId}`, null);
  }

  unsaveJob(jobId: number): Observable<void> {
    this._savedIds.delete(jobId);
    return this.http.delete<void>(`${this.API}/${jobId}`);
  }

  isSaved(jobId: number): boolean {
    return this._savedIds.has(jobId);
  }
}
