import { Routes } from '@angular/router';
import { CompetitionListComponent } from './components/competition-list/competition-list.component';
import { CompetitionFormComponent } from './components/competition-form/competition-form.component';
import { TrainingPlanOverviewComponent } from './components/training-plan-overview/training-plan-overview.component';
import { TrainingCompletionComponent } from './components/training-completion/training-completion.component';
import { TrainingPlanUploadComponent } from './components/training-plan-upload/training-plan-upload.component';
import { StravaCallbackComponent } from './components/strava-callback/strava-callback.component';
import { ProfileComponent } from './components/profile/profile.component';
import { LandingPageComponent } from './components/landing-page/landing-page.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { PlanLibraryComponent } from './components/plan-library/plan-library.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', component: LandingPageComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'strava/callback', component: StravaCallbackComponent },
  { path: 'competitions', component: CompetitionListComponent, canActivate: [authGuard] },
  { path: 'competitions/new', component: CompetitionFormComponent, canActivate: [authGuard] },
  { path: 'competitions/:id/edit', component: CompetitionFormComponent, canActivate: [authGuard] },
  { path: 'competitions/:id/upload', component: TrainingPlanUploadComponent, canActivate: [authGuard] },
  { path: 'plans', component: PlanLibraryComponent, canActivate: [authGuard] },
  { path: 'overview', component: TrainingPlanOverviewComponent, canActivate: [authGuard] },
  { path: 'completion', component: TrainingCompletionComponent, canActivate: [authGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
