import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Badge, Button, Alert, Spinner, Form, ButtonGroup } from 'react-bootstrap';
import { trainingPlanAPI, trainingAPI, competitionAPI } from '../services/api';

const TrainingPlanOverview = () => {
  const [competitions, setCompetitions] = useState([]);
  const [selectedCompetitions, setSelectedCompetitions] = useState([]);
  const [trainingPlans, setTrainingPlans] = useState({});
  const [weekTrainings, setWeekTrainings] = useState({});
  const [currentDate, setCurrentDate] = useState(new Date());
  const [showEmptyDays, setShowEmptyDays] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    loadCompetitions();
  }, []);

  useEffect(() => {
    if (selectedCompetitions.length > 0) {
      loadTrainingPlansAndTrainings();
    }
  }, [selectedCompetitions, currentDate]);

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyPress = (event) => {
      if (event.target.tagName === 'INPUT') return; // Ignore if in input field
      
      switch(event.key) {
        case 'ArrowLeft':
          event.preventDefault();
          goToPreviousWeek();
          break;
        case 'ArrowRight':
          event.preventDefault();
          goToNextWeek();
          break;
        case 't':
        case 'T':
          event.preventDefault();
          goToToday();
          break;
        default:
          break;
      }
    };

    document.addEventListener('keydown', handleKeyPress);
    return () => document.removeEventListener('keydown', handleKeyPress);
  }, [currentDate]);

  const loadCompetitions = async () => {
    try {
      const response = await competitionAPI.getAll();
      setCompetitions(response.data);
    } catch (error) {
      setError('Fehler beim Laden der Wettkämpfe');
    }
  };

  const loadTrainingPlansAndTrainings = async () => {
    setLoading(true);
    setError('');
    
    try {
      const plansData = {};
      const weekTrainingsData = {};
      const weekStart = getWeekStart(currentDate);
      const weekEnd = getWeekEnd(currentDate);

      for (const competitionId of selectedCompetitions) {
        // Lade Trainingspläne für jeden Wettkampf
        const plansResponse = await trainingPlanAPI.getByCompetition(competitionId);
        plansData[competitionId] = plansResponse.data;

        // Lade Trainings für die ganze Woche
        weekTrainingsData[competitionId] = {};
        
        // Lade Trainings für jeden Tag der Woche
        const weekDays = getWeekDays(weekStart);
        for (const day of weekDays) {
          const dateString = formatDateForAPI(day);
          try {
            const trainingsResponse = await trainingAPI.getByCompetitionAndDate(competitionId, dateString);
            weekTrainingsData[competitionId][dateString] = trainingsResponse.data;
          } catch (err) {
            // Keine Trainings für dieses Datum
            weekTrainingsData[competitionId][dateString] = [];
          }
        }
      }

      setTrainingPlans(plansData);
      setWeekTrainings(weekTrainingsData);
    } catch (error) {
      setError('Fehler beim Laden der Trainingsdaten');
    } finally {
      setLoading(false);
    }
  };

  const handleCompetitionToggle = (competitionId) => {
    setSelectedCompetitions(prev => 
      prev.includes(competitionId)
        ? prev.filter(id => id !== competitionId)
        : [...prev, competitionId]
    );
  };

  // Navigation zwischen Wochen
  const goToPreviousWeek = () => {
    const newDate = new Date(currentDate);
    newDate.setDate(newDate.getDate() - 7);
    setCurrentDate(newDate);
  };

  const goToNextWeek = () => {
    const newDate = new Date(currentDate);
    newDate.setDate(newDate.getDate() + 7);
    setCurrentDate(newDate);
  };

  const goToToday = () => {
    setCurrentDate(new Date());
  };

  // Wochen-Hilfsfunktionen
  const getWeekStart = (date) => {
    const start = new Date(date);
    const day = start.getDay();
    const diff = start.getDate() - day + (day === 0 ? -6 : 1); // Montag als Wochenstart
    start.setDate(diff);
    start.setHours(0, 0, 0, 0);
    return start;
  };

  const getWeekEnd = (date) => {
    const end = new Date(getWeekStart(date));
    end.setDate(end.getDate() + 6);
    end.setHours(23, 59, 59, 999);
    return end;
  };

  const getWeekDays = (weekStart) => {
    const days = [];
    for (let i = 0; i < 7; i++) {
      const day = new Date(weekStart);
      day.setDate(weekStart.getDate() + i);
      days.push(day);
    }
    return days;
  };

  const getWeekNumber = (date) => {
    const start = new Date(date.getFullYear(), 0, 1);
    const days = Math.floor((date - start) / (24 * 60 * 60 * 1000));
    return Math.ceil((days + start.getDay() + 1) / 7);
  };

  // Datum-Formatierung
  const formatDateForDisplay = (date) => {
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    const dateString = date.toDateString();
    const todayString = today.toDateString();
    const yesterdayString = yesterday.toDateString();
    const tomorrowString = tomorrow.toDateString();

    if (dateString === todayString) {
      return 'Heute';
    } else if (dateString === yesterdayString) {
      return 'Gestern';
    } else if (dateString === tomorrowString) {
      return 'Morgen';
    } else {
      return date.toLocaleDateString('de-DE', { 
        weekday: 'long', 
        year: 'numeric', 
        month: 'long', 
        day: 'numeric' 
      });
    }
  };

  const formatDateShort = (date) => {
    return date.toLocaleDateString('de-DE', {
      weekday: 'short',
      month: 'short',
      day: 'numeric'
    });
  };

  const isToday = (date) => {
    const today = new Date();
    return date.toDateString() === today.toDateString();
  };

  // Lokale Datumsformatierung ohne Timezone-Probleme
  const formatDateForAPI = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  // Farben für verschiedene Wettkämpfe
  const getCompetitionColor = (competitionId) => {
    const colors = [
      '#007bff', // Blau
      '#28a745', // Grün  
      '#dc3545', // Rot
      '#ffc107', // Gelb
      '#6f42c1', // Lila
      '#fd7e14', // Orange
      '#20c997', // Teal
      '#e83e8c'  // Pink
    ];
    
    const index = selectedCompetitions.indexOf(competitionId);
    return colors[index % colors.length];
  };

  const formatTrainingType = (type) => {
    const typeMap = {
      'endurance': 'Ausdauer',
      'interval': 'Intervall',
      'strength': 'Kraft',
      'race': 'Wettkampf',
      'fartlek': 'Fahrtspiel',
      'recovery': 'Regeneration',
      'swimming': 'Schwimmen',
      'cycling': 'Radfahren',
      'general': 'Allgemein'
    };
    return typeMap[type] || type;
  };

  const getIntensityColor = (intensity) => {
    const intensityColors = {
      'high': 'danger',
      'medium': 'warning',
      'low': 'info',
      'recovery': 'success',
      'rest': 'secondary'
    };
    return intensityColors[intensity] || 'secondary';
  };

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 className="mb-0">Trainingsplan Übersicht</h2>
          <small className="text-muted">
            Navigiere mit ← → zwischen Wochen oder drücke 'T' für diese Woche
          </small>
        </div>
        {!isToday(currentDate) && (
          <Button variant="outline-primary" size="sm" onClick={goToToday}>
            Zu Heute springen (T)
          </Button>
        )}
      </div>

      {/* Wochen-Navigation */}
      <Card className="mb-4">
        <Card.Body>
          <div className="d-flex justify-content-between align-items-center mb-4">
            <ButtonGroup className="navigation-buttons">
              <Button variant="outline-secondary" onClick={goToPreviousWeek}>
                ← Vorherige Woche
              </Button>
              <Button variant="outline-secondary" onClick={goToNextWeek}>
                Nächste Woche →
              </Button>
            </ButtonGroup>
            
            <div className="text-center mx-3">
              <h4 className={`mb-0 ${isToday(currentDate) ? 'text-primary' : ''}`}>
                KW {getWeekNumber(currentDate)} - {currentDate.getFullYear()}
              </h4>
              <small className="text-muted">
                {getWeekStart(currentDate).toLocaleDateString('de-DE')} - {getWeekEnd(currentDate).toLocaleDateString('de-DE')}
              </small>
            </div>
            
            <div className="d-flex gap-2">
              {/* Schnell-Navigation für benachbarte Wochen */}
              {[-2, -1, 1, 2].map(offset => {
                const date = new Date(currentDate);
                date.setDate(date.getDate() + (offset * 7));
                return (
                  <Button
                    key={offset}
                    variant="outline-light"
                    size="sm"
                    className="quick-nav-btn"
                    onClick={() => setCurrentDate(date)}
                    title={`KW ${getWeekNumber(date)}`}
                  >
                    KW {getWeekNumber(date)}
                  </Button>
                );
              })}
            </div>
          </div>

          <hr />
          
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h6 className="mb-0">Wettkämpfe auswählen</h6>
            <Form.Check
              type="switch"
              id="show-empty-days"
              label="Leere Tage anzeigen"
              checked={showEmptyDays}
              onChange={(e) => setShowEmptyDays(e.target.checked)}
              className="ms-3"
            />
          </div>
          
          <Form>
            {competitions.map(competition => (
              <Form.Check
                key={competition.id}
                type="checkbox"
                id={`competition-${competition.id}`}
                label={`${competition.name} (${new Date(competition.date).toLocaleDateString('de-DE')})`}
                checked={selectedCompetitions.includes(competition.id)}
                onChange={() => handleCompetitionToggle(competition.id)}
                className="mb-2"
              />
            ))}
          </Form>
        </Card.Body>
      </Card>

      {error && <Alert variant="danger">{error}</Alert>}

      {loading && (
        <div className="text-center my-4">
          <Spinner animation="border" />
          <p className="mt-2">Lade Trainingsdaten...</p>
        </div>
      )}

      {selectedCompetitions.length === 0 && !loading && (
        <Alert variant="info">
          Bitte wähle mindestens einen Wettkampf aus, um die Trainingspläne zu vergleichen.
        </Alert>
      )}

      {selectedCompetitions.length > 0 && !loading && (
        <div className="week-unified-view">
          <Card className="mb-4">
            <Card.Header className="bg-secondary text-white">
              <div className="d-flex justify-content-between align-items-center">
                <div>
                  <h5 className="mb-0">Trainingsplan Vergleich</h5>
                  <small>
                    {selectedCompetitions.map(competitionId => {
                      const competition = competitions.find(c => c.id === competitionId);
                      return competition?.name;
                    }).join(' • ')}
                  </small>
                </div>
                <div>
                  <Badge bg="light" text="dark">
                    {selectedCompetitions.length} Wettkampf{selectedCompetitions.length !== 1 ? 'e' : ''}
                  </Badge>
                </div>
              </div>
            </Card.Header>
            
            <Card.Body className="p-3">
              <div className="daily-rows">
                {(() => {
                  const weekStart = getWeekStart(currentDate);
                  const weekDays = getWeekDays(weekStart);
                  
                  return weekDays
                    .filter(day => {
                      if (showEmptyDays) return true;
                      // Prüfe ob IRGENDEIN Wettkampf an diesem Tag Trainings hat
                      const dateString = formatDateForAPI(day);
                      return selectedCompetitions.some(competitionId => {
                        const competitionWeekTrainings = weekTrainings[competitionId] || {};
                        const dayTrainings = competitionWeekTrainings[dateString] || [];
                        return dayTrainings.length > 0;
                      });
                    })
                    .map((day) => {
                      const dayName = ['Montag', 'Dienstag', 'Mittwoch', 'Donnerstag', 'Freitag', 'Samstag', 'Sonntag'][day.getDay() === 0 ? 6 : day.getDay() - 1];
                      const dayShort = ['Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa', 'So'][day.getDay() === 0 ? 6 : day.getDay() - 1];
                      const dateString = formatDateForAPI(day);
                      const isToday = day.toDateString() === new Date().toDateString();
                      
                      // Sammle alle Trainings aller Wettkämpfe für diesen Tag
                      const allDayTrainings = [];
                      selectedCompetitions.forEach(competitionId => {
                        const competition = competitions.find(c => c.id === competitionId);
                        const competitionWeekTrainings = weekTrainings[competitionId] || {};
                        const dayTrainings = competitionWeekTrainings[dateString] || [];
                        dayTrainings.forEach(training => {
                          allDayTrainings.push({
                            ...training,
                            competitionName: competition?.name,
                            competitionId: competitionId,
                            competitionColor: getCompetitionColor(competitionId)
                          });
                        });
                      });
                      
                      return (
                        <div key={dateString} className={`daily-row ${isToday ? 'today' : ''} ${allDayTrainings.length === 0 ? 'empty' : ''}`}>
                          <div className="day-label">
                            <div className="day-name">
                              <strong>{dayName}</strong>
                              <span className="day-short d-md-none">{dayShort}</span>
                            </div>
                            <div className="day-date">
                              {day.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit' })}
                            </div>
                            {isToday && <Badge bg="primary" className="today-badge">Heute</Badge>}
                          </div>
                          
                          <div className="day-trainings-horizontal">
                            {allDayTrainings.length === 0 ? (
                              <div className="no-training-placeholder">
                                <span className="text-muted">Kein Training</span>
                              </div>
                            ) : (
                              <div className="trainings-row">
                                {allDayTrainings.map((training, index) => (
                                  <div key={`${training.competitionId}-${training.id}`} 
                                       className={`training-card intensity-${training.intensityLevel}`}
                                       style={{ borderLeftColor: training.competitionColor }}>
                                    <div className="training-header">
                                      <div className="training-name">{training.name}</div>
                                      <Badge bg={getIntensityColor(training.intensityLevel)} className="intensity-badge">
                                        {training.intensityLevel}
                                      </Badge>
                                    </div>
                                    
                                    <div className="training-details">
                                      <div className="training-type">
                                        {formatTrainingType(training.trainingType)}
                                      </div>
                                      {training.durationMinutes && (
                                        <div className="training-duration">
                                          {training.durationMinutes} Min.
                                        </div>
                                      )}
                                    </div>
                                    
                                    <div className="training-description" title={training.description}>
                                      {training.description}
                                    </div>
                                    
                                    <div className="competition-label" style={{ borderColor: training.competitionColor, color: training.competitionColor }}>
                                      {training.competitionName}
                                    </div>
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                        </div>
                      );
                    });
                })()}
              </div>
            </Card.Body>
          </Card>
        </div>
      )}
    </div>
  );
};

export default TrainingPlanOverview;