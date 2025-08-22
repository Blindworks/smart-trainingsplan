import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TrainingPlanOverviewComponent } from './training-plan-overview.component';

describe('TrainingPlanOverviewComponent', () => {
  let component: TrainingPlanOverviewComponent;
  let fixture: ComponentFixture<TrainingPlanOverviewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TrainingPlanOverviewComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TrainingPlanOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
