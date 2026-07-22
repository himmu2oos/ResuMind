import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { ApiService } from "../../core/services/api.service";
import { Project } from "../../core/models/profile.model";

@Component({
  selector: "app-projects",
  standalone: true,
  imports: [CommonModule],
  templateUrl: "./projects.component.html",
  styleUrl: "./projects.component.scss",
})
export class ProjectsComponent implements OnInit {
  all: Project[] = [];
  filtered: Project[] = [];
  categories = ["All", "Full Stack", "Frontend", "Backend", "ML/AI"];
  selected = "All";

  constructor(private api: ApiService) {}
  //ngOnInit(): void { this.api.getProjects().subscribe((d) => { this.all = d; this.filtered = d; }); }
  ngOnInit(): void {
    this.api.getProjects().subscribe((d) => {
      console.log("Projects:", d);
      this.all = d;
      this.filtered = d;
    });
  }

  filter(cat: string): void {
    this.selected = cat;
    this.filtered =
      cat === "All" ? this.all : this.all.filter((p) => p.category === cat);
  }
}
