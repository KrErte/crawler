import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Profile, ProfileUpdateRequest, CvAnalysis } from '../models/profile.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly API = `${environment.apiUrl}/api/profile`;

  constructor(private http: HttpClient) {}

  getProfile(): Observable<Profile> {
    return this.http.get<Profile>(this.API);
  }

  updateProfile(req: ProfileUpdateRequest): Observable<Profile> {
    return this.http.put<Profile>(this.API, req);
  }

  uploadCv(file: File): Observable<Profile> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Profile>(`${this.API}/cv`, formData);
  }

  downloadCv(): Observable<Blob> {
    return this.http.get(`${this.API}/cv`, { responseType: 'blob' });
  }

  deleteCv(): Observable<void> {
    return this.http.delete<void>(`${this.API}/cv`);
  }

  importLinkedIn(file: File): Observable<Profile> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Profile>(`${this.API}/import-linkedin`, formData);
  }

  buildCv(data: any): Observable<Blob> {
    return this.http.post(`${this.API}/cv/build`, data, { responseType: 'blob' });
  }

  buildAndSaveCv(data: any): Observable<Profile> {
    return this.http.post<Profile>(`${this.API}/cv/build-and-save`, data);
  }

  getCvAnalysis(): Observable<CvAnalysis> {
    return this.http.get<CvAnalysis>(`${this.API}/cv-analysis`);
  }
}
