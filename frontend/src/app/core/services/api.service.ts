import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { BehaviorSubject,Observable } from "rxjs";
import {
  Profile,
  Education,
  WorkExperience,
  Project,
  ChatRequest,
  ChatResponse,
  ChatMessage,
} from "../models/profile.model";
import { environment } from "../../../environments/environment";

@Injectable({ providedIn: "root" })
export class ApiService {
  private base = environment.apiUrl;

  private profileSubject = new BehaviorSubject<Profile | null>(null);
  profile$ = this.profileSubject.asObservable();

  constructor(private http: HttpClient) {}

  getProfile(): Observable<Profile> {
    return this.http.get<Profile>(`${this.base}/profile`);
  }

  loadProfile(): void {
    this.getProfile().subscribe({
      next: (p) => this.profileSubject.next(p),
      error: () => this.profileSubject.next(null),
    });
  }

  getEducation(): Observable<Education[]> {
    return this.http.get<Education[]>(`${this.base}/education`);
  }

  getWorkHistory(): Observable<WorkExperience[]> {
    return this.http.get<WorkExperience[]>(`${this.base}/work-history`);
  }

  getProjects(category?: string): Observable<Project[]> {
    if (category) {
      return this.http.get<Project[]>(`${this.base}/projects`, {
        params: { category },
      });
    }
    return this.http.get<Project[]>(`${this.base}/projects`);
  }

  getFeaturedProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.base}/projects/featured`);
  }

  getProject(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.base}/projects/${id}`);
  }

  uploadResume(file: File, adminKey: string): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.base}/document/upload`, formData, {
        headers: {'X-Admin-Key': adminKey},
    });
  }

  getDocumentStatus(): Observable<any> {
    return this.http.get(`${this.base}/document/status`);
  }

  sendChat(request: ChatRequest): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.base}/chat`, request);
  }

  getChatHistory(sessionId: string): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(
      `${this.base}/chat/history/${sessionId}`,
    );
  }
}
