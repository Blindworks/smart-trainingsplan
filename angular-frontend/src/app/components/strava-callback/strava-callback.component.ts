import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-strava-callback',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule, MatSnackBarModule],
  template: `
    <div style="display:flex;flex-direction:column;align-items:center;justify-content:center;height:80vh;gap:20px;">
      <mat-spinner diameter="48"></mat-spinner>
      <p style="color:var(--text-secondary,#aaa);font-size:14px;">Verbinde mit Strava...</p>
    </div>
  `
})
export class StravaCallbackComponent implements OnInit {
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const stravaParam = this.route.snapshot.queryParamMap.get('strava');
    if (stravaParam === 'connected') {
      this.snackBar.open('Strava verbunden! ✓', 'Schließen', { duration: 4000 });
    }
    this.router.navigate(['/overview']);
  }
}
