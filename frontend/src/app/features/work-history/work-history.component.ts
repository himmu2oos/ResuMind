import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';
import { WorkExperience } from '../../core/models/profile.model';

@Component({
  selector: 'app-work-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './work-history.component.html',
  styleUrl: './work-history.component.scss',
})
export class WorkHistoryComponent implements OnInit {
  work: WorkExperience[] = [];
  constructor(private api: ApiService) {}
  ngOnInit(): void { this.api.getWorkHistory().subscribe((d) => (this.work = d)); }

  fmt(d: string): string {
    if (!d) return '';
    const [y, m] = d.split('-');
    return ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'][+m - 1] + ' ' + y;
  }
}
