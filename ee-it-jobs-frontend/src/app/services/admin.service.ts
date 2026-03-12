import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface UserListDto {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  isActive: boolean;
  isAdmin: boolean;
  createdAt: string;
  applicationCount: number;
}

export interface UserPage {
  content: UserListDto[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface JobUpdateRequest {
  title?: string;
  company?: string;
  description?: string;
  skills?: string[];
  isActive?: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly API = `${environment.apiUrl}/api/admin`;

  constructor(private http: HttpClient) {}

  getUsers(page: number = 0, size: number = 20, search?: string): Observable<UserPage> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    if (search) params = params.set('search', search);
    return this.http.get<UserPage>(`${this.API}/users`, { params });
  }

  getUser(id: number): Observable<UserListDto> {
    return this.http.get<UserListDto>(`${this.API}/users/${id}`);
  }

  toggleUserActive(id: number, active: boolean): Observable<void> {
    return this.http.put<void>(`${this.API}/users/${id}/toggle-active`, null, {
      params: { active }
    });
  }

  toggleUserAdmin(id: number, admin: boolean): Observable<void> {
    return this.http.put<void>(`${this.API}/users/${id}/toggle-admin`, null, {
      params: { admin }
    });
  }

  updateJob(id: number, request: JobUpdateRequest): Observable<void> {
    return this.http.put<void>(`${this.API}/jobs/${id}`, request);
  }

  deleteJob(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/jobs/${id}`);
  }
}
