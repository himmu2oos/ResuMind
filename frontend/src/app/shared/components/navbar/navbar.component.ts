import { Component } from "@angular/core";
import { CommonModule } from "@angular/common";
import { RouterLink, RouterLinkActive } from "@angular/router";
import { Profile } from "@core/models/profile.model";
import { ApiService } from "@core/services/api.service";

@Component({
  selector: "app-navbar",
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar">
      <a routerLink="/" class="brand">
        <span class="brand-mark">◆</span> {{ name }}
      </a>
      <button class="menu-btn" (click)="open = !open">
        {{ open ? "✕" : "☰" }}
      </button>
      <div class="links" [class.open]="open">
        <a routerLink="/about" routerLinkActive="active" (click)="open = false"
          >About</a
        >
        <a
          routerLink="/education"
          routerLinkActive="active"
          (click)="open = false"
          >Education</a
        >
        <a
          routerLink="/experience"
          routerLinkActive="active"
          (click)="open = false"
          >Experience</a
        >
        <a
          routerLink="/projects"
          routerLinkActive="active"
          (click)="open = false"
          >Projects</a
        >
        <a routerLink="/upload" routerLinkActive="active" (click)="open = false"
          >Upload</a
        >
        <a
          routerLink="/chat"
          routerLinkActive="active"
          class="chat-link"
          (click)="open = false"
          >🤖 Ask AI</a
        >
      </div>
    </nav>
  `,
  styles: [
    `
      .navbar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0 2rem;
        height: 64px;
        border-bottom: 1px solid var(--color-border);
        background: rgba(255, 255, 255, 0.92);
        backdrop-filter: blur(12px);
        position: sticky;
        top: 0;
        z-index: 100;
      }
      .brand {
        font-weight: 800;
        font-size: 1.1rem;
        text-decoration: none;
        color: var(--color-text);
        display: flex;
        align-items: center;
        gap: 0.4rem;
      }
      .brand-mark {
        color: var(--color-accent);
        font-size: 1.3rem;
      }
      .links {
        display: flex;
        align-items: center;
        gap: 0.25rem;
      }
      .links a {
        padding: 0.5rem 1rem;
        text-decoration: none;
        color: var(--color-text-muted);
        font-size: 0.9rem;
        font-weight: 500;
        border-radius: 8px;
        transition: all 0.2s;
      }
      .links a:hover {
        color: var(--color-text);
        background: var(--color-surface);
      }
      .links a.active {
        color: var(--color-accent);
        background: var(--color-accent-light);
      }
      .chat-link {
        background: var(--color-accent) !important;
        color: #fff !important;
        border-radius: 100px !important;
        padding: 0.5rem 1.25rem !important;
      }
      .chat-link:hover {
        opacity: 0.9;
      }
      .menu-btn {
        display: none;
        background: none;
        border: none;
        font-size: 1.5rem;
      }
      @media (max-width: 768px) {
        .menu-btn {
          display: block;
        }
        .links {
          display: none;
          position: absolute;
          top: 64px;
          left: 0;
          right: 0;
          flex-direction: column;
          background: #fff;
          padding: 1rem;
          border-bottom: 1px solid var(--color-border);
          box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
        }
        .links.open {
          display: flex;
        }
        .links a {
          width: 100%;
          text-align: center;
          padding: 0.75rem;
        }
      }
    `,
  ],
})
export class NavbarComponent {
  open = false;
  name: string | null = "Your Name";

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.profile$.subscribe((p) =>{
        if (p) this.name = p.fullName;
    });
    this.api.loadProfile();
  }
}
