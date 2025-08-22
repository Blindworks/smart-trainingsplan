import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TrainingPlanUploadComponent } from './training-plan-upload.component';

describe('TrainingPlanUploadComponent', () => {
  let component: TrainingPlanUploadComponent;
  let fixture: ComponentFixture<TrainingPlanUploadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TrainingPlanUploadComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TrainingPlanUploadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
