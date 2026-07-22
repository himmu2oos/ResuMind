import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/home/home.component').then((m) => m.HomeComponent),
    title: 'Home | AI Recruiter Portal',
  },
  {
    path: 'about',
    loadComponent: () =>
      import('./features/about/about.component').then((m) => m.AboutComponent),
    title: 'About | AI Recruiter Portal',
  },
  {
    path: 'education',
    loadComponent: () =>
      import('./features/education/education.component').then((m) => m.EducationComponent),
    title: 'Education | AI Recruiter Portal',
  },
  {
    path: 'experience',
    loadComponent: () =>
      import('./features/work-history/work-history.component').then((m) => m.WorkHistoryComponent),
    title: 'Experience | AI Recruiter Portal',
  },
  {
    path: 'projects',
    loadComponent: () =>
      import('./features/projects/projects.component').then((m) => m.ProjectsComponent),
    title: 'Projects | AI Recruiter Portal',
  },
  {
    path: 'chat',
    loadComponent: () =>
      import('./features/ai-chat/ai-chat.component').then((m) => m.AiChatComponent),
    title: 'Ask AI | AI Recruiter Portal',
  },
  {
    path: 'upload',
    loadComponent: () =>
      import('./features/upload/upload.component').then((m) => m.UploadComponent),
    title: 'Upload Resume | AI Recruiter Portal',
  },
  { path: '**', redirectTo: '' },
];
