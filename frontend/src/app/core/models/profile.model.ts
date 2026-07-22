export interface Profile {
  id: number;
  fullName: string;
  title: string;
  summary: string;
  email: string;
  phone?: string;
  location: string;
  photoUrl?: string;
  linkedinUrl: string;
  githubUrl: string;
  portfolioUrl?: string;
  skills: string[];
  languages: string[];
}

export interface Education {
  id: number;
  institution: string;
  degree: string;
  fieldOfStudy?: string;
  startDate: string;
  endDate?: string;
  gpa?: number;
  description?: string;
  logoUrl?: string;
  sortOrder: number;
}

export interface WorkExperience {
  id: number;
  company: string;
  jobTitle: string;
  location?: string;
  startDate: string;
  endDate?: string;
  isCurrent: boolean;
  description: string;
  highlights: string[];
  technologies: string[];
  companyLogoUrl?: string;
  sortOrder: number;
}

export interface Project {
  id: number;
  name: string;
  description: string;
  githubUrl?: string;
  liveUrl?: string;
  imageUrl?: string;
  techStack: string[];
  category: string;
  featured: boolean;
  stars?: number;
  forks?: number;
  sortOrder: number;
}

export interface ChatRequest {
  message: string;
  sessionId?: string;
}

export interface ChatResponse {
  reply: string;
  sessionId: string;
  timestamp: number;
  rateLimited: boolean;
}

export interface ChatMessage {
  id?: number;
  sessionId: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  timestamp?: string;
}
