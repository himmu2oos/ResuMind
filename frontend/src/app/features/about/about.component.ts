import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { RouterLink } from "@angular/router";
import { ApiService } from "../../core/services/api.service";
import { Profile } from "../../core/models/profile.model";

@Component({
  selector: "app-about",
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: "./about.component.html",
  styleUrl: "./about.component.scss",
})
export class AboutComponent implements OnInit {
  profile: Profile | null = null;
  constructor(private api: ApiService) {}
  ngOnInit(): void {
    this.api.getProfile().subscribe((p) => {
      console.log("Profile data:", p);
      this.profile = p;
    });
  }
}
