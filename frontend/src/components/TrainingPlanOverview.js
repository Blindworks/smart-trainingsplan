import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Badge, Button, Alert, Spinner, Form } from 'react-bootstrap';
import { trainingPlanAPI, trainingAPI, competitionAPI } from '../services/api';

const TrainingPlanOverview = () => {
  const [competitions, setCompetitions] = useState([]);
  const [selectedCompetitions, setSelectedCompetitions] = useState([]);
  const [trainingPlans, setTrainingPlans] = useState({});
  const [trainings, setTrainings] = useState({});
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    loadCompetitions();
  }, []);

  useEffect(() => {
    if (selectedCompetitions.length > 0) {
      loadTrainingPlansAndTrainings();
    }
  }, [selectedCompetitions, selectedDate]);

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
      const trainingsData = {};

      for (const competitionId of selectedCompetitions) {
        // Lade Trainingspläne für jeden Wettkampf
        const plansResponse = await trainingPlanAPI.getByCompetition(competitionId);
        plansData[competitionId] = plansResponse.data;

        // Lade Trainings für das ausgewählte Datum
        try {
          const trainingsResponse = await trainingAPI.getByCompetitionAndDate(competitionId, selectedDate);
          trainingsData[competitionId] = trainingsResponse.data;
        } catch (err) {
          // Keine Trainings für dieses Datum
          trainingsData[competitionId] = [];
        }
      }

      setTrainingPlans(plansData);
      setTrainings(trainingsData);
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
      <h2 className="mb-4">Trainingsplan Übersicht</h2>
      
      <Card className="mb-4">
        <Card.Body>
          <h5>Wettkämpfe auswählen</h5>
          <Form className="mb-3">
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
          
          <Form.Group className="mb-3">
            <Form.Label>Datum auswählen</Form.Label>
            <Form.Control
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              style={{ maxWidth: '200px' }}
            />
          </Form.Group>
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
        <Row>
          {selectedCompetitions.map(competitionId => {
            const competition = competitions.find(c => c.id === competitionId);
            const plans = trainingPlans[competitionId] || [];
            const competitionTrainings = trainings[competitionId] || [];

            return (
              <Col key={competitionId} md={6} lg={4} className="mb-4">
                <Card className="h-100">
                  <Card.Header className="bg-primary text-white">
                    <h5 className="mb-0">{competition?.name}</h5>
                    <small>{new Date(competition?.date).toLocaleDateString('de-DE')}</small>
                  </Card.Header>
                  <Card.Body>
                    <div className="mb-3">
                      <Badge bg="info" className="me-2">
                        {plans.length} Trainingsplan{plans.length !== 1 ? 'e' : ''}
                      </Badge>
                      <Badge bg="success">
                        {competitionTrainings.length} Training{competitionTrainings.length !== 1 ? 's' : ''}
                      </Badge>
                    </div>

                    <h6>Trainings am {new Date(selectedDate).toLocaleDateString('de-DE')}:</h6>
                    
                    {competitionTrainings.length === 0 ? (
                      <Alert variant="light" className="small">
                        Keine Trainings für dieses Datum
                      </Alert>
                    ) : (
                      <div className="training-list">
                        {competitionTrainings.map(training => (
                          <Card key={training.id} className="mb-2 border-left">
                            <Card.Body className="p-3">
                              <div className="d-flex justify-content-between align-items-start mb-2">
                                <strong className="text-truncate me-2">{training.name}</strong>
                                <Badge bg={getIntensityColor(training.intensityLevel)}>
                                  {training.intensityLevel}
                                </Badge>
                              </div>
                              
                              <p className="small text-muted mb-2">
                                {formatTrainingType(training.trainingType)}
                                {training.durationMinutes && (
                                  <span> • {training.durationMinutes} Min.</span>
                                )}
                              </p>
                              
                              <p className="small mb-0 text-truncate">
                                {training.description}
                              </p>
                              
                              {training.trainingPlan && (
                                <small className="text-muted">
                                  Plan: {training.trainingPlan.name}
                                </small>
                              )}
                            </Card.Body>
                          </Card>
                        ))}
                      </div>
                    )}

                    <hr />
                    
                    <h6>Verfügbare Trainingspläne:</h6>
                    {plans.length === 0 ? (
                      <Alert variant="light" className="small">
                        Keine Trainingspläne vorhanden
                      </Alert>
                    ) : (
                      <div className="plan-list">
                        {plans.map(plan => (
                          <div key={plan.id} className="mb-2">
                            <div className="d-flex justify-content-between align-items-center">
                              <span className="small font-weight-bold">{plan.name}</span>
                              <Badge bg="outline-secondary" className="small">
                                {new Date(plan.uploadDate).toLocaleDateString('de-DE')}
                              </Badge>
                            </div>
                            {plan.description && (
                              <p className="small text-muted mb-0">{plan.description}</p>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </Card.Body>
                </Card>
              </Col>
            );
          })}
        </Row>
      )}
    </div>
  );
};

export default TrainingPlanOverview;