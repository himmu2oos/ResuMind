import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';
import { Education } from '../../core/models/profile.model';

@Component({
  selector: 'app-education',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './education.component.html',
  styleUrl: './education.component.scss',
})
export class EducationComponent implements OnInit {
  education: Education[] = [];
  constructor(private api: ApiService) {}
  ngOnInit(): void { this.api.getEducation().subscribe((d) => (this.education = d)); }

  fmt(d: string): string {
    if (!d) return '';
    const [y, m] = d.split('-');
    return ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'][+m - 1] + ' ' + y;
  }
}
