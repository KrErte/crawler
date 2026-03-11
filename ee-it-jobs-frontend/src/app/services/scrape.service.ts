import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ScrapeService {
  private readonly API = `${environment.apiUrl}/api/scrape`;

  constructor(private http: HttpClient) {}

  triggerScrape(): Observable<any> {
    return this.http.post(`${this.API}/trigger`, {});
  }

  getStatus(): Observable<any> {
    return this.http.get(`${this.API}/status`);
  }
}
