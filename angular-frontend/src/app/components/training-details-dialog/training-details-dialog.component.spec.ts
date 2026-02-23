import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { TrainingDetailsDialogComponent } from './training-details-dialog.component';
import { Training } from '../../models/competition.model';

describe('TrainingDetailsDialogComponent', () => {
  let component: TrainingDetailsDialogComponent;
  let fixture: ComponentFixture<TrainingDetailsDialogComponent>;

  const mockTraining: Training = {
    id: 1,
    name: 'Test Training',
    trainingDate: '2024-01-15',
    durationMinutes: 90,
    intensityLevel: 'high',
    trainingType: 'endurance',
    isCompleted: false,
    description: 'Test description',
    trainingPlanName: 'Test Plan'
  };

  const mockDialogRef = {
    close: jasmine.createSpy('close')
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        TrainingDetailsDialogComponent,
        NoopAnimationsModule
      ],
      providers: [
        { provide: MatDialogRef, useValue: mockDialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { training: mockTraining } }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TrainingDetailsDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with training data', () => {
    expect(component.training).toEqual(mockTraining);
  });

  it('should close dialog when onClose is called', () => {
    component.onClose();
    expect(mockDialogRef.close).toHaveBeenCalled();
  });

  it('should format date correctly', () => {
    const formattedDate = component.formatDate('2024-01-15');
    expect(formattedDate).toContain('15');
    expect(formattedDate).toContain('Januar');
    expect(formattedDate).toContain('2024');
  });

  it('should format duration correctly', () => {
    expect(component.formatDuration(90)).toBe('1h 30min');
    expect(component.formatDuration(45)).toBe('45min');
    expect(component.formatDuration(undefined)).toBe('Nicht angegeben');
  });

  it('should get correct training type color', () => {
    expect(component.getTrainingTypeColor('endurance')).toBe('#2196f3');
    expect(component.getTrainingTypeColor(undefined)).toBe('#9e9e9e');
  });

  it('should get correct intensity color', () => {
    expect(component.getIntensityColor('high')).toBe('#f44336');
    expect(component.getIntensityColor(undefined)).toBe('#9e9e9e');
  });

  it('should detect detailed information correctly', () => {
    component.training.trainingDescription = {
      id: 1,
      name: 'Test Description',
      detailedInstructions: 'Test instructions'
    };
    expect(component.hasDetailedInformation()).toBeTruthy();

    component.training.trainingDescription = undefined;
    expect(component.hasDetailedInformation()).toBeFalsy();
  });

  it('should get correct completion status', () => {
    component.training.isCompleted = true;
    expect(component.getCompletionStatusText()).toBe('Abgeschlossen');
    expect(component.getCompletionStatusIcon()).toBe('check_circle');
    expect(component.getCompletionStatusColor()).toBe('#4caf50');

    component.training.isCompleted = false;
    expect(component.getCompletionStatusText()).toBe('Geplant');
    expect(component.getCompletionStatusIcon()).toBe('schedule');
    expect(component.getCompletionStatusColor()).toBe('#ff9800');
  });
});