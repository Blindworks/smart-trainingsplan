import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TrainingCompletionComponent } from './training-completion.component';

describe('TrainingCompletionComponent', () => {
  let component: TrainingCompletionComponent;
  let fixture: ComponentFixture<TrainingCompletionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TrainingCompletionComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TrainingCompletionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
