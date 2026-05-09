import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

import { RunClubService } from '../../services/run-club.service';
import { CreatePostRequest, RunClubPost } from '../../models/run-club.model';

@Component({
  selector: 'app-run-club-post-composer',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './run-club-post-composer.html',
  styleUrl: './run-club-post-composer.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RunClubPostComposer {
  private readonly service = inject(RunClubService);

  @Input({ required: true }) clubId!: number;
  @Output() posted = new EventEmitter<RunClubPost>();

  content = signal('');
  posting = signal(false);
  error = signal<string | null>(null);

  linkedActivityId = signal<number | null>(null);
  linkedRouteId = signal<number | null>(null);
  linkedEventId = signal<number | null>(null);

  submit(): void {
    const text = this.content().trim();
    if (!text || this.posting()) return;

    this.posting.set(true);
    this.error.set(null);

    const req: CreatePostRequest = {
      content: text,
      linkedActivityId: this.linkedActivityId(),
      linkedCommunityRouteId: this.linkedRouteId(),
      linkedGroupEventId: this.linkedEventId()
    };

    this.service.createPost(this.clubId, req).subscribe({
      next: post => {
        this.posting.set(false);
        this.content.set('');
        this.linkedActivityId.set(null);
        this.linkedRouteId.set(null);
        this.linkedEventId.set(null);
        this.posted.emit(post);
      },
      error: () => {
        this.posting.set(false);
        this.error.set('SAVE_ERROR');
      }
    });
  }

  promptLinkActivity(): void {
    const v = window.prompt('Activity ID');
    if (v) this.linkedActivityId.set(Number(v) || null);
  }

  promptLinkRoute(): void {
    const v = window.prompt('Route ID');
    if (v) this.linkedRouteId.set(Number(v) || null);
  }

  promptLinkEvent(): void {
    const v = window.prompt('Event ID');
    if (v) this.linkedEventId.set(Number(v) || null);
  }

  clearLinks(): void {
    this.linkedActivityId.set(null);
    this.linkedRouteId.set(null);
    this.linkedEventId.set(null);
  }

  hasLink(): boolean {
    return !!(this.linkedActivityId() || this.linkedRouteId() || this.linkedEventId());
  }
}
