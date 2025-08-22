import { Routes } from '@angular/router';
import { CompetitionListComponent } from './components/competition-list/competition-list.component';
import { CompetitionFormComponent } from './components/competition-form/competition-form.component';
import { TrainingPlanOverviewComponent } from './components/training-plan-overview/training-plan-overview.component';
import { TrainingCompletionComponent } from './components/training-completion/training-completion.component';
import { TrainingPlanUploadComponent } from './components/training-plan-upload/training-plan-upload.component';

export const routes: Routes = [
  { path: '', redirectTo: '/competitions', pathMatch: 'full' },
  { path: 'competitions', component: CompetitionListComponent },
  { path: 'competitions/new', component: CompetitionFormComponent },
  { path: 'competitions/:id/edit', component: CompetitionFormComponent },
  { path: 'competitions/:id/upload', component: TrainingPlanUploadComponent },
  { path: 'overview', component: TrainingPlanOverviewComponent },
  { path: 'completion', component: TrainingCompletionComponent },
  { path: '**', redirectTo: '/competitions' }
];
