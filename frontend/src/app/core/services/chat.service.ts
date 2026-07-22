import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { ApiService } from './api.service';
import { ChatMessage, ChatResponse } from '../models/profile.model';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private sessionId: string;
  private messagesSubject = new BehaviorSubject<ChatMessage[]>([]);
  private loadingSubject = new BehaviorSubject<boolean>(false);

  messages$ = this.messagesSubject.asObservable();
  loading$ = this.loadingSubject.asObservable();

  constructor(private api: ApiService) {
    this.sessionId = this.getOrCreateSession();
  }

  getSessionId(): string {
    return this.sessionId;
  }

  sendMessage(content: string): Observable<ChatResponse> {
    const userMsg: ChatMessage = {
      sessionId: this.sessionId,
      role: 'USER',
      content,
      timestamp: new Date().toISOString(),
    };
    this.messagesSubject.next([...this.messagesSubject.value, userMsg]);
    this.loadingSubject.next(true);

    return this.api.sendChat({ message: content, sessionId: this.sessionId }).pipe(
      tap({
        next: (res) => {
          const assistantMsg: ChatMessage = {
            sessionId: this.sessionId,
            role: 'ASSISTANT',
            content: res.reply,
            timestamp: new Date(res.timestamp).toISOString(),
          };
          this.messagesSubject.next([...this.messagesSubject.value, assistantMsg]);
          this.loadingSubject.next(false);
        },
        error: () => {
          const errMsg: ChatMessage = {
            sessionId: this.sessionId,
            role: 'ASSISTANT',
            content: "Sorry, I'm having trouble responding. Please try again or explore the portfolio directly.",
            timestamp: new Date().toISOString(),
          };
          this.messagesSubject.next([...this.messagesSubject.value, errMsg]);
          this.loadingSubject.next(false);
        },
      })
    );
  }

  clearChat(): void {
    this.sessionId = 'sess_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
    try { sessionStorage.setItem('chat_session', this.sessionId); } catch {}
    this.messagesSubject.next([]);
  }

  private getOrCreateSession(): string {
    try {
      const existing = sessionStorage.getItem('chat_session');
      if (existing) return existing;
    } catch {}
    const id = 'sess_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
    try { sessionStorage.setItem('chat_session', id); } catch {}
    return id;
  }
}
