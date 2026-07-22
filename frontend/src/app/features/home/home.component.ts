import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { Profile, Project, WorkExperience } from '../../core/models/profile.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  profile: Profile | null = null;
  featured: Project[] = [];
  latestWork: WorkExperience | null = null;
  skillCount = 0;
  workCount = 0;
  totalProjects = 0;
  educationCount = 0;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.getProfile().subscribe((p) => {
      this.profile = p;
      this.skillCount = p.skills?.length ?? 0;
    });
    this.api.getFeaturedProjects().subscribe((p) => (this.featured = p));
    this.api.getProjects().subscribe((p) => (this.totalProjects = p.length));
    this.api.getWorkHistory().subscribe((w) => {
      this.workCount = w.length;
      this.latestWork = w.find((j) => j.isCurrent) || w[0] || null;
    });
    this.api.getEducation().subscribe((e) => (this.educationCount = e.length));
  }
}
