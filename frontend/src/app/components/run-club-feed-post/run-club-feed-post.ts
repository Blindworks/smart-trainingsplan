import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
  inject,
  signal
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

import { RunClubService } from '../../services/run-club.service';
import { RunClubComment, RunClubPost } from '../../models/run-club.model';

@Component({
  selector: 'app-run-club-feed-post',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TranslateModule, DatePipe],
  templateUrl: './run-club-feed-post.html',
  styleUrl: './run-club-feed-post.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RunClubFeedPost {
  private readonly service = inject(RunClubService);

  @Input({ required: true }) post!: RunClubPost;
  @Input() canDelete = false;
  @Output() deleted = new EventEmitter<number>();

  liking = signal(false);
  showComments = signal(false);
  comments = signal<RunClubComment[]>([]);
  loadingComments = signal(false);
  newComment = signal('');
  commentSubmitting = signal(false);

  toggleLike(): void {
    if (this.liking()) return;
    this.liking.set(true);
    const wasLiked = this.post.likedByMe;
    const op = wasLiked ? this.service.unlikePost(this.post.id) : this.service.likePost(this.post.id);
    op.subscribe({
      next: () => {
        this.post.likedByMe = !wasLiked;
        this.post.likeCount += wasLiked ? -1 : 1;
        this.liking.set(false);
      },
      error: () => this.liking.set(false)
    });
  }

  toggleComments(): void {
    const open = !this.showComments();
    this.showComments.set(open);
    if (open && this.comments().length === 0) {
      this.loadComments();
    }
  }

  loadComments(): void {
    this.loadingComments.set(true);
    this.service.getComments(this.post.id).subscribe({
      next: cs => {
        this.comments.set(cs);
        this.loadingComments.set(false);
      },
      error: () => this.loadingComments.set(false)
    });
  }

  submitComment(): void {
    const text = this.newComment().trim();
    if (!text || this.commentSubmitting()) return;
    this.commentSubmitting.set(true);
    this.service.createComment(this.post.id, text).subscribe({
      next: c => {
        this.comments.set([...this.comments(), c]);
        this.post.commentCount += 1;
        this.newComment.set('');
        this.commentSubmitting.set(false);
      },
      error: () => this.commentSubmitting.set(false)
    });
  }

  deleteComment(c: RunClubComment): void {
    if (!confirm('Delete comment?')) return;
    this.service.deleteComment(c.id).subscribe({
      next: () => {
        this.comments.set(this.comments().filter(x => x.id !== c.id));
        this.post.commentCount = Math.max(0, this.post.commentCount - 1);
      }
    });
  }

  delete(): void {
    if (!confirm('Delete post?')) return;
    this.service.deletePost(this.post.id).subscribe({
      next: () => this.deleted.emit(this.post.id)
    });
  }
}
