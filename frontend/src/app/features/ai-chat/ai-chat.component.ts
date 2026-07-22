import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../core/services/chat.service';
import { ChatMessage } from '../../core/models/profile.model';

@Component({
  selector: 'app-ai-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-chat.component.html',
  styleUrl: './ai-chat.component.scss',
})
export class AiChatComponent implements OnInit, AfterViewChecked {
  @ViewChild('msgBox') msgBox!: ElementRef;
  msg = '';
  messages$ = this.chat.messages$;
  loading$ = this.chat.loading$;

  suggestions = [
    "What's their strongest skill?",
    'Tell me about their work experience',
    'What projects have they built?',
    'Why would they be a good senior hire?',
    'What technologies do they know?',
    "What's their education?",
  ];

  constructor(private chat: ChatService) {}
  ngOnInit(): void {}
  ngAfterViewChecked(): void { this.scroll(); }

  send(): void {
    const m = this.msg.trim();
    if (!m) return;
    this.msg = '';
    this.chat.sendMessage(m).subscribe();
  }

  ask(q: string): void { this.msg = q; this.send(); }
  clear(): void { this.chat.clearChat(); }

  private scroll(): void {
    try { const el = this.msgBox?.nativeElement; if (el) el.scrollTop = el.scrollHeight; } catch {}
  }
}
