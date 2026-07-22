import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  template: `
    <footer>
      <p>Built with Angular + Spring Boot · AI-Powered · &copy; {{ year }} Your Name</p>
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
export class FooterComponent {
  year = new Date().getFullYear();
}
