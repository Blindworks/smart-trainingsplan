import { ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, OnDestroy, Output, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

import { RunClubService } from '../../services/run-club.service';
import { CreatePostRequest, RunClubPost } from '../../models/run-club.model';

interface PendingImage {
  file: File;
  previewUrl: string;
}

@Component({
  selector: 'app-run-club-post-composer',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslateModule],
  templateUrl: './run-club-post-composer.html',
  styleUrl: './run-club-post-composer.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RunClubPostComposer implements OnDestroy {
  private readonly service = inject(RunClubService);

  @Input({ required: true }) clubId!: number;
  @Output() posted = new EventEmitter<RunClubPost>();

  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;

  content = signal('');
  posting = signal(false);
  error = signal<string | null>(null);

  linkedActivityId = signal<number | null>(null);
  linkedRouteId = signal<number | null>(null);
  linkedEventId = signal<number | null>(null);

  selectedImages = signal<PendingImage[]>([]);

  ngOnDestroy(): void {
    for (const img of this.selectedImages()) URL.revokeObjectURL(img.previewUrl);
  }

  openFilePicker(): void {
    this.fileInput?.nativeElement.click();
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const additions: PendingImage[] = [];
    for (let i = 0; i < input.files.length; i++) {
      const f = input.files.item(i);
      if (!f) continue;
      additions.push({ file: f, previewUrl: URL.createObjectURL(f) });
    }
    this.selectedImages.set([...this.selectedImages(), ...additions]);
    input.value = '';
  }

  removeImage(index: number): void {
    const list = this.selectedImages();
    const removed = list[index];
    if (removed) URL.revokeObjectURL(removed.previewUrl);
    this.selectedImages.set(list.filter((_, i) => i !== index));
  }

  canSubmit(): boolean {
    return !this.posting() && (this.content().trim().length > 0 || this.selectedImages().length > 0);
  }

  submit(): void {
    if (!this.canSubmit()) return;

    this.posting.set(true);
    this.error.set(null);

    const req: CreatePostRequest = {
      content: this.content().trim(),
      linkedActivityId: this.linkedActivityId(),
      linkedCommunityRouteId: this.linkedRouteId(),
      linkedGroupEventId: this.linkedEventId()
    };
    const files = this.selectedImages().map(p => p.file);

    this.service.createPost(this.clubId, req, files).subscribe({
      next: post => {
        this.posting.set(false);
        this.content.set('');
        this.linkedActivityId.set(null);
        this.linkedRouteId.set(null);
        this.linkedEventId.set(null);
        for (const img of this.selectedImages()) URL.revokeObjectURL(img.previewUrl);
        this.selectedImages.set([]);
        this.posted.emit(post);
      },
      error: err => {
        this.posting.set(false);
        const serverMsg = err?.error?.message ?? err?.message ?? null;
        this.error.set(serverMsg ? `${err.status ?? ''} ${serverMsg}`.trim() : 'Failed to save post');
        console.error('Run club post create failed', err);
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
