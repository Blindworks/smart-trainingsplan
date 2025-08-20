import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Card, Row, Col, Alert, Button, Form, Badge } from 'react-bootstrap';
import { competitionAPI, trainingPlanAPI, trainingAPI } from '../services/api';
import TrainingCard from './TrainingCard';
import moment from 'moment';

const TrainingWeekView = () => {
  const { id: competitionId } = useParams();
  const [competition, setCompetition] = useState(null);
  const [trainingPlans, setTrainingPlans] = useState([]);
  const [selectedPlans, setSelectedPlans] = useState([]);
  const [selectedDate, setSelectedDate] = useState(moment().format('YYYY-MM-DD'));
  const [trainings, setTrainings] = useState([]);
  const [mixedTrainings, setMixedTrainings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showMixed, setShowMixed] = useState(false);

  useEffect(() => {
    loadData();
  }, [competitionId]);

  useEffect(() => {
    if (selectedDate) {
      loadTrainingsForDate();
    }
  }, [selectedDate, competitionId]);

  useEffect(() => {
    if (selectedPlans.length > 1 && selectedDate) {
      loadMixedTrainings();
    } else {
      setMixedTrainings([]);
      setShowMixed(false);
    }
  }, [selectedPlans, selectedDate]);

  const loadData = async () => {
    try {
      const [compResponse, plansResponse] = await Promise.all([
        competitionAPI.getById(competitionId),
        trainingPlanAPI.getByCompetition(competitionId)
      ]);

      setCompetition(compResponse.data);
      setTrainingPlans(plansResponse.data);
    } catch (error) {
      console.error('Error loading data:', error);
      setError('Fehler beim Laden der Daten.');
    } finally {
      setLoading(false);
    }
  };

  const loadTrainingsForDate = async () => {
    try {
      const response = await trainingAPI.getByCompetitionAndDate(competitionId, selectedDate);
      setTrainings(response.data);
    } catch (error) {
      console.error('Error loading trainings:', error);
      setTrainings([]);
    }
  };

  const loadMixedTrainings = async () => {
    try {
      const response = await trainingAPI.getMixed(competitionId, selectedPlans, selectedDate);
      setMixedTrainings(response.data);
    } catch (error) {
      console.error('Error loading mixed trainings:', error);
      setMixedTrainings([]);
    }
  };

  const handlePlanSelection = (planId) => {
    setSelectedPlans(prev => 
      prev.includes(planId) 
        ? prev.filter(id => id !== planId)
        : [...prev, planId]
    );
  };

  const handleTrainingFeedback = async (trainingId, isCompleted, status) => {
    try {
      await trainingAPI.updateFeedback(trainingId, {
        isCompleted,
        completionStatus: status
      });
      
      loadTrainingsForDate();
    } catch (error) {
      console.error('Error updating training feedback:', error);
      setError('Fehler beim Aktualisieren des Trainings.');
    }
  };

  const getIntensityColor = (intensity) => {
    switch (intensity?.toLowerCase()) {
      case 'high': return 'danger';
      case 'medium': return 'warning';
      case 'low': return 'success';
      default: return 'secondary';
    }
  };

  if (loading) {
    return <div className="text-center">Lade Trainingswochen...</div>;
  }

  if (!competition) {
    return <Alert variant="danger">Wettkampf nicht gefunden.</Alert>;
  }

  return (
    <div>
      <div className="mb-4">
        <h2>{competition.name}</h2>
        <p className="text-muted">
          Wettkampfdatum: {moment(competition.date).format('DD.MM.YYYY')} 
          ({moment(competition.date).diff(moment(), 'days')} Tage)
        </p>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      <Row>
        <Col md={3}>
          <Card className="mb-4">
            <Card.Header>
              <h5>Trainingsplaner</h5>
            </Card.Header>
            <Card.Body>
              <Form.Group className="mb-3">
                <Form.Label>Datum auswählen</Form.Label>
                <Form.Control
                  type="date"
                  value={selectedDate}
                  onChange={(e) => setSelectedDate(e.target.value)}
                  min={moment().format('YYYY-MM-DD')}
                  max={moment(competition.date).format('YYYY-MM-DD')}
                />
              </Form.Group>

              {trainingPlans.length > 0 && (
                <>
                  <Form.Label>Trainingspläne auswählen</Form.Label>
                  {trainingPlans.map(plan => (
                    <Form.Check
                      key={plan.id}
                      type="checkbox"
                      id={`plan-${plan.id}`}
                      label={plan.name}
                      checked={selectedPlans.includes(plan.id)}
                      onChange={() => handlePlanSelection(plan.id)}
                      className="mb-2"
                    />
                  ))}

                  {selectedPlans.length > 1 && (
                    <Button
                      variant={showMixed ? "success" : "outline-success"}
                      size="sm"
                      className="mt-2"
                      onClick={() => setShowMixed(!showMixed)}
                    >
                      {showMixed ? 'Zeige normale Trainings' : 'Zeige Mischtraining'}
                    </Button>
                  )}
                </>
              )}
            </Card.Body>
          </Card>
        </Col>

        <Col md={9}>
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h4>
              Trainings für {moment(selectedDate).format('DD.MM.YYYY')}
            </h4>
            {selectedPlans.length > 1 && (
              <Badge bg={showMixed ? "success" : "primary"}>
                {showMixed ? 'Mischtraining' : `${selectedPlans.length} Pläne ausgewählt`}
              </Badge>
            )}
          </div>

          {showMixed && mixedTrainings.length > 0 ? (
            <div className="mixed-training-suggestion mb-4">
              <h5 className="text-white mb-3">🎯 Mischtraining-Vorschlag</h5>
              <Row>
                {mixedTrainings.map((training, index) => (
                  <Col md={6} key={index} className="mb-3">
                    <Card className="border-0">
                      <Card.Body>
                        <div className="d-flex justify-content-between align-items-start mb-2">
                          <h6>{training.name}</h6>
                          <Badge bg={getIntensityColor(training.intensityLevel)}>
                            {training.intensityLevel}
                          </Badge>
                        </div>
                        <p className="small text-muted mb-2">{training.description}</p>
                        {training.durationMinutes && (
                          <small className="text-muted">
                            Dauer: {training.durationMinutes} Min
                          </small>
                        )}
                      </Card.Body>
                    </Card>
                  </Col>
                ))}
              </Row>
            </div>
          ) : (
            <>
              {trainings.length === 0 ? (
                <Alert variant="info">
                  Keine Trainings für dieses Datum gefunden. 
                  {trainingPlans.length === 0 && (
                    <div className="mt-2">
                      <Button 
                        variant="primary" 
                        size="sm"
                        onClick={() => window.location.href = `/competitions/${competitionId}/upload`}
                      >
                        Trainingsplan hochladen
                      </Button>
                    </div>
                  )}
                </Alert>
              ) : (
                <Row>
                  {trainings.map(training => (
                    <Col md={6} lg={4} key={training.id} className="mb-3">
                      <TrainingCard
                        training={training}
                        onFeedback={handleTrainingFeedback}
                      />
                    </Col>
                  ))}
                </Row>
              )}
            </>
          )}

          {trainingPlans.length === 0 && (
            <Alert variant="warning">
              <Alert.Heading>Keine Trainingspläne vorhanden</Alert.Heading>
              <p>
                Laden Sie zunächst einen oder mehrere Trainingspläne hoch, 
                um Ihre wöchentlichen Trainings zu planen.
              </p>
              <Button 
                variant="primary"
                onClick={() => window.location.href = `/competitions/${competitionId}/upload`}
              >
                Trainingsplan hochladen
              </Button>
            </Alert>
          )}
        </Col>
      </Row>
    </div>
  );
};

export default TrainingWeekView;