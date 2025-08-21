import React, { useState, useEffect, useRef } from 'react';
import { Card, Row, Col, Badge, Button, Alert, Spinner, Form, ButtonGroup, Modal } from 'react-bootstrap';
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
  
  // FIT File Upload States
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [selectedUploadDate, setSelectedUploadDate] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [uploadedTrainings, setUploadedTrainings] = useState({});
  const fileInputRef = useRef(null);

  // Training Details Modal States
  const [showTrainingDetails, setShowTrainingDetails] = useState(null);

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
      // Select all competitions by default
      setSelectedCompetitions(response.data.map(competition => competition.id));
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
      const completedTrainingsData = {};

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

      // Lade Completed Trainings für die ganze Woche
      const weekDays = getWeekDays(weekStart);
      for (const day of weekDays) {
        const dateString = formatDateForAPI(day);
        try {
          const completedResponse = await trainingAPI.getCompletedTrainingsByDate(dateString);
          if (completedResponse.data && completedResponse.data.length > 0) {
            // Konvertiere Backend-Daten in Frontend-Format
            const completedTraining = completedResponse.data[0]; // Nehme das erste (sollte nur eines pro Tag geben)
            completedTrainingsData[dateString] = {
              distance: completedTraining.distanceKm || 0,
              duration: completedTraining.durationSeconds || 0,
              averageHeartRate: completedTraining.averageHeartRate || 0,
              maxHeartRate: completedTraining.maxHeartRate || 0,
              averagePace: completedTraining.averagePaceSecondsPerKm || 0,
              calories: completedTraining.calories || 0,
              elevationGain: completedTraining.elevationGainM || 0,
              filename: completedTraining.originalFilename || 'Uploaded Training'
            };
          }
        } catch (err) {
          // Keine Completed Trainings für dieses Datum
        }
      }

      setTrainingPlans(plansData);
      setWeekTrainings(weekTrainingsData);
      setUploadedTrainings(completedTrainingsData);
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

  // FIT File Upload Funktionen
  const handleUploadClick = (date) => {
    setSelectedUploadDate(date);
    setShowUploadModal(true);
  };

  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    if (!file.name.toLowerCase().endsWith('.fit')) {
      setError('Bitte wähle eine .FIT-Datei aus');
      return;
    }

    setUploading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('date', formatDateForAPI(selectedUploadDate));

      // API-Call zum Backend
      const response = await trainingAPI.uploadFitFile(formData);
      const trainingData = response.data;
      
      // Konvertiere Backend-Daten in Frontend-Format
      const fitData = {
        distance: trainingData.distanceKm || 0,
        duration: trainingData.durationSeconds || 0,
        averageHeartRate: trainingData.averageHeartRate || 0,
        maxHeartRate: trainingData.maxHeartRate || 0,
        averagePace: trainingData.averagePaceSecondsPerKm || 0,
        calories: trainingData.calories || 0,
        elevationGain: trainingData.elevationGainM || 0,
        filename: trainingData.originalFilename || file.name
      };

      setUploadedTrainings(prev => ({
        ...prev,
        [formatDateForAPI(selectedUploadDate)]: fitData
      }));

      setShowUploadModal(false);
      setError('');
      
      // Optional: Lade Completed Trainings für das spezifische Datum neu, um sicherzustellen dass die Daten synchron sind
      try {
        const dateString = formatDateForAPI(selectedUploadDate);
        const completedResponse = await trainingAPI.getCompletedTrainingsByDate(dateString);
        if (completedResponse.data && completedResponse.data.length > 0) {
          const completedTraining = completedResponse.data[0];
          const refreshedData = {
            distance: completedTraining.distanceKm || 0,
            duration: completedTraining.durationSeconds || 0,
            averageHeartRate: completedTraining.averageHeartRate || 0,
            maxHeartRate: completedTraining.maxHeartRate || 0,
            averagePace: completedTraining.averagePaceSecondsPerKm || 0,
            calories: completedTraining.calories || 0,
            elevationGain: completedTraining.elevationGainM || 0,
            filename: completedTraining.originalFilename || file.name
          };
          setUploadedTrainings(prev => ({
            ...prev,
            [dateString]: refreshedData
          }));
        }
      } catch (refreshError) {
        // Ignore refresh errors, the local data is already updated
      }
    } catch (error) {
      setError('Fehler beim Hochladen der .FIT-Datei');
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const formatDuration = (seconds) => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    
    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
  };

  const formatPace = (seconds) => {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}:${secs.toString().padStart(2, '0')} min/km`;
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
                            
                            <Button 
                              variant="outline-success" 
                              size="sm" 
                              className="upload-btn mt-2"
                              onClick={() => handleUploadClick(day)}
                              title="Training hochladen (.FIT)"
                            >
                              📊 Upload
                            </Button>
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
                                      {training.trainingDescription && (
                                        <Button 
                                          variant="outline-info" 
                                          size="sm" 
                                          className="details-btn mt-1"
                                          onClick={() => setShowTrainingDetails(training)}
                                        >
                                          Details
                                        </Button>
                                      )}
                                    </div>
                                    
                                    <div className="training-description" title={training.trainingDescription?.name || ''}>
                                      {training.trainingDescription?.name || 'Kein Trainingsplan verfügbar'}
                                    </div>
                                    
                                    <div className="competition-label" style={{ borderColor: training.competitionColor, color: training.competitionColor }}>
                                      {training.competitionName}
                                    </div>
                                  </div>
                                ))}
                                
                                {/* FIT File Statistiken anzeigen */}
                                {uploadedTrainings[dateString] && (
                                  <div className="fit-stats-card">
                                    <div className="fit-header">
                                      <div className="fit-title">📊 Durchgeführt</div>
                                      <Badge bg="success" className="completed-badge">
                                        Abgeschlossen
                                      </Badge>
                                    </div>
                                    
                                    <div className="fit-stats-grid">
                                      <div className="stat-item">
                                        <div className="stat-value">{uploadedTrainings[dateString].distance} km</div>
                                        <div className="stat-label">Distanz</div>
                                      </div>
                                      <div className="stat-item">
                                        <div className="stat-value">{formatDuration(uploadedTrainings[dateString].duration)}</div>
                                        <div className="stat-label">Zeit</div>
                                      </div>
                                      <div className="stat-item">
                                        <div className="stat-value">{formatPace(uploadedTrainings[dateString].averagePace)}</div>
                                        <div className="stat-label">⌀ Pace</div>
                                      </div>
                                      <div className="stat-item">
                                        <div className="stat-value">{uploadedTrainings[dateString].averageHeartRate}</div>
                                        <div className="stat-label">⌀ HF</div>
                                      </div>
                                      <div className="stat-item">
                                        <div className="stat-value">{uploadedTrainings[dateString].calories}</div>
                                        <div className="stat-label">kcal</div>
                                      </div>
                                      <div className="stat-item">
                                        <div className="stat-value">{uploadedTrainings[dateString].elevationGain}m</div>
                                        <div className="stat-label">Höhenmeter</div>
                                      </div>
                                    </div>
                                    
                                    <div className="fit-filename">
                                      📁 {uploadedTrainings[dateString].filename}
                                    </div>
                                  </div>
                                )}
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
      
      {/* Upload Modal */}
      <Modal show={showUploadModal} onHide={() => setShowUploadModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Training hochladen</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {selectedUploadDate && (
            <div className="mb-3">
              <p><strong>Datum:</strong> {selectedUploadDate.toLocaleDateString('de-DE', { 
                weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' 
              })}</p>
            </div>
          )}
          
          <Form.Group>
            <Form.Label>
              <strong>.FIT-Datei auswählen</strong>
              <small className="text-muted d-block">
                Unterstützte Formate: Garmin, Polar, Suunto (.fit Dateien)
              </small>
            </Form.Label>
            <Form.Control
              type="file"
              accept=".fit"
              onChange={handleFileUpload}
              disabled={uploading}
              ref={fileInputRef}
            />
          </Form.Group>
          
          {uploading && (
            <div className="text-center mt-3">
              <Spinner animation="border" size="sm" className="me-2" />
              <span>Datei wird verarbeitet...</span>
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowUploadModal(false)} disabled={uploading}>
            Abbrechen
          </Button>
        </Modal.Footer>
      </Modal>

      {/* Training Details Modal */}
      <Modal show={!!showTrainingDetails} onHide={() => setShowTrainingDetails(null)} centered size="xl">
        <Modal.Header closeButton className="bg-primary text-white">
          <Modal.Title className="d-flex align-items-center">
            <span className="me-2">🏃‍♂️</span>
            {showTrainingDetails?.name || 'Training Details'}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body className="p-0">
          {showTrainingDetails && (
            <div>
              {/* Header Section with Badges */}
              <div className="bg-light p-4 border-bottom">
                <div className="d-flex flex-wrap gap-2 justify-content-center">
                  <Badge bg={getIntensityColor(showTrainingDetails.intensityLevel)} className="px-3 py-2 fs-6">
                    <i className="bi bi-speedometer2 me-1"></i>
                    {showTrainingDetails.intensityLevel.toUpperCase()}
                  </Badge>
                  <Badge bg="secondary" className="px-3 py-2 fs-6">
                    <i className="bi bi-tag me-1"></i>
                    {formatTrainingType(showTrainingDetails.trainingType)}
                  </Badge>
                  {showTrainingDetails.durationMinutes && (
                    <Badge bg="info" className="px-3 py-2 fs-6">
                      <i className="bi bi-clock me-1"></i>
                      {showTrainingDetails.durationMinutes} Min.
                    </Badge>
                  )}
                  {showTrainingDetails.trainingDescription?.difficultyLevel && (
                    <Badge bg="warning" text="dark" className="px-3 py-2 fs-6">
                      <i className="bi bi-star me-1"></i>
                      {showTrainingDetails.trainingDescription.difficultyLevel}
                    </Badge>
                  )}
                </div>
              </div>

              {showTrainingDetails.trainingDescription && (
                <div className="p-4">
                  {/* Main Instructions - Prominently Featured */}
                  {showTrainingDetails.trainingDescription.detailedInstructions && (
                    <Card className="mb-4 border-primary shadow-sm">
                      <Card.Header className="bg-primary text-white">
                        <h5 className="mb-0 d-flex align-items-center">
                          <i className="bi bi-list-ul me-2"></i>
                          {showTrainingDetails.trainingDescription.name}
                        </h5>
                        <small className="text-white-50">Trainingsanweisungen</small>
                      </Card.Header>
                      <Card.Body className="p-4">
                        <div className="training-instructions" style={{
                          fontSize: '1.1rem',
                          lineHeight: '1.6',
                          color: '#2c3e50',
                          backgroundColor: '#f8f9fa',
                          border: '1px solid #dee2e6',
                          borderRadius: '8px',
                          padding: '20px'
                        }}>
                          {showTrainingDetails.trainingDescription.detailedInstructions}
                        </div>
                      </Card.Body>
                    </Card>
                  )}

                  {/* Quick Info Grid */}
                  {(showTrainingDetails.trainingDescription.estimatedDurationMinutes || 
                    showTrainingDetails.trainingDescription.equipment) && (
                    <Row className="mb-4">
                      {showTrainingDetails.trainingDescription.estimatedDurationMinutes && (
                        <Col md={6}>
                          <Card className="h-100 border-success">
                            <Card.Body className="text-center">
                              <div className="display-6 text-success mb-2">⏱️</div>
                              <h6 className="text-success">Geschätzte Dauer</h6>
                              <h4 className="mb-0">{showTrainingDetails.trainingDescription.estimatedDurationMinutes} Min</h4>
                            </Card.Body>
                          </Card>
                        </Col>
                      )}
                      
                      {showTrainingDetails.trainingDescription.equipment && (
                        <Col md={6}>
                          <Card className="h-100 border-info">
                            <Card.Body>
                              <div className="d-flex align-items-center mb-2">
                                <span className="display-6 text-info me-2">🎯</span>
                                <h6 className="text-info mb-0">Equipment</h6>
                              </div>
                              <p className="mb-0" style={{fontSize: '0.95rem', lineHeight: '1.4'}}>
                                {showTrainingDetails.trainingDescription.equipment}
                              </p>
                            </Card.Body>
                          </Card>
                        </Col>
                      )}
                    </Row>
                  )}

                  {/* Warmup & Cooldown */}
                  {(showTrainingDetails.trainingDescription.warmupInstructions || 
                    showTrainingDetails.trainingDescription.cooldownInstructions) && (
                    <Row className="mb-4">
                      {showTrainingDetails.trainingDescription.warmupInstructions && (
                        <Col md={6}>
                          <Card className="h-100 border-warning">
                            <Card.Header className="bg-warning text-dark">
                              <h6 className="mb-0">
                                <i className="bi bi-fire me-2"></i>
                                Aufwärmen
                              </h6>
                            </Card.Header>
                            <Card.Body>
                              <p className="mb-0" style={{fontSize: '0.95rem', lineHeight: '1.4'}}>
                                {showTrainingDetails.trainingDescription.warmupInstructions}
                              </p>
                            </Card.Body>
                          </Card>
                        </Col>
                      )}
                      
                      {showTrainingDetails.trainingDescription.cooldownInstructions && (
                        <Col md={6}>
                          <Card className="h-100 border-info">
                            <Card.Header className="bg-info text-white">
                              <h6 className="mb-0">
                                <i className="bi bi-snow me-2"></i>
                                Abwärmen
                              </h6>
                            </Card.Header>
                            <Card.Body>
                              <p className="mb-0" style={{fontSize: '0.95rem', lineHeight: '1.4'}}>
                                {showTrainingDetails.trainingDescription.cooldownInstructions}
                              </p>
                            </Card.Body>
                          </Card>
                        </Col>
                      )}
                    </Row>
                  )}

                  {/* Tips Section */}
                  {showTrainingDetails.trainingDescription.tips && (
                    <Card className="border-success">
                      <Card.Header className="bg-success text-white">
                        <h6 className="mb-0">
                          <i className="bi bi-lightbulb me-2"></i>
                          Tipps & Hinweise
                        </h6>
                      </Card.Header>
                      <Card.Body className="bg-light">
                        <div className="d-flex">
                          <div className="text-success me-3" style={{fontSize: '1.5rem'}}>💡</div>
                          <p className="mb-0" style={{fontSize: '1rem', lineHeight: '1.5', fontStyle: 'italic'}}>
                            {showTrainingDetails.trainingDescription.tips}
                          </p>
                        </div>
                      </Card.Body>
                    </Card>
                  )}
                </div>
              )}

              {!showTrainingDetails.trainingDescription && (
                <div className="p-4 text-center text-muted">
                  <i className="bi bi-info-circle display-1 mb-3"></i>
                  <h5>Keine detaillierten Anweisungen verfügbar</h5>
                  <p>Für dieses Training sind noch keine spezifischen Anleitungen hinterlegt.</p>
                </div>
              )}
            </div>
          )}
        </Modal.Body>
        <Modal.Footer className="bg-light">
          <Button 
            variant="outline-secondary" 
            onClick={() => setShowTrainingDetails(null)}
            className="px-4"
          >
            <i className="bi bi-x-circle me-2"></i>
            Schließen
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default TrainingPlanOverview;