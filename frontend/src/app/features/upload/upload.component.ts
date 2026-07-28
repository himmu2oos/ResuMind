import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './upload.component.html',
  styleUrl: './upload.component.scss',
})
export class UploadComponent implements OnInit {
  status: any = null;
  uploading = false;
  uploadResult: any = null;
  error: string | null = null;
  dragOver = false;
  selectedFile: File | null = null;
  adminKey = '';

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.loadStatus();
  }

  loadStatus(): void {
    this.api.getDocumentStatus().subscribe({
      next: (s) => (this.status = s),
      error: () => (this.status = null),
    });
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.selectFile(files[0]);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectFile(input.files[0]);
    }
  }

  selectFile(file: File): void {
    const allowed = ['.pdf', '.docx', '.txt'];
    const ext = file.name.toLowerCase().substring(file.name.lastIndexOf('.'));
    if (!allowed.includes(ext)) {
      this.error = 'Unsupported file type. Please upload a PDF, DOCX, or TXT file.';
      return;
    }
    this.selectedFile = file;
    this.error = null;
    this.uploadResult = null;
  }

  upload(): void {
    if (!this.selectedFile) return;

    this.uploading = true;
    this.error = null;
    this.uploadResult = null;

    this.api.uploadResume(this.selectedFile, this.adminKey).subscribe({
      next: (result) => {
        this.uploading = false;
        this.uploadResult = result;
        this.selectedFile = null;
        this.adminKey = '';
        this.loadStatus();
        this.api.loadProfile();
      },
      error: (err) => {
        this.uploading = false;
        this.error = err.error?.message || 'Upload failed. Please try again.';
        if (err.status === 401){
            this.adminKey = '';
        }
      },
    });
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  }
}
