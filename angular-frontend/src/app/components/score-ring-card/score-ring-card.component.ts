import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';

export type ScoreRingState = 'good' | 'warn' | 'bad' | 'info';

@Component({
  selector: 'app-score-ring-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './score-ring-card.component.html',
  styleUrl: './score-ring-card.component.scss'
})
export class ScoreRingCardComponent implements OnInit, OnChanges {
  @Input() value = 0;
  @Input() max = 100;
  @Input() label = '';
  @Input() subtitle = '';
  @Input() state: ScoreRingState = 'info';
  @Input() footerText?: string;
  @Input() infoText?: string;

  readonly radius = 52;
  readonly circumference = 2 * Math.PI * this.radius;

  dashOffset = this.circumference;
  infoOpen = false;

  ngOnInit(): void {
    this.animateRing();
  }

  ngOnChanges(): void {
    this.animateRing();
  }

  get normalizedProgress(): number {
    if (this.max <= 0) {
      return 0;
    }

    return Math.max(0, Math.min(this.value / this.max, 1));
  }

  get valueDisplay(): string {
    if (this.max <= 3) {
      return this.value.toFixed(2);
    }

    if (this.max <= 21) {
      return this.value.toFixed(1);
    }

    return Math.round(this.value).toString();
  }

  toggleInfo(): void {
    this.infoOpen = !this.infoOpen;
  }

  private animateRing(): void {
    this.dashOffset = this.circumference;

    requestAnimationFrame(() => {
      this.dashOffset = this.circumference * (1 - this.normalizedProgress);
    });
  }
}
