import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '@core/services/api.service';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <footer>
      <p>Built with Angular + Spring Boot · AI-Powered · &copy; {{ year }} {{ name }}</p>
    </footer>
  `,
  styles: [`
    footer {
      padding: 1.5rem 2rem;
      text-align: center;
      border-top: 1px solid var(--color-border);
      color: var(--color-text-muted);
      font-size: 0.8rem;
    }
  `],
})
export class FooterComponent implements OnInit {
  year = new Date().getFullYear();

  name = 'Your Name';

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.profile$.subscribe((p) => {
      if (p) this.name = p.fullName;
    });
  }
}
