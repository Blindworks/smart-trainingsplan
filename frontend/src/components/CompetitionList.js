import React, { useState, useEffect } from 'react';
import { Card, Button, Row, Col, Alert, Badge } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { competitionAPI } from '../services/api';
import moment from 'moment';

const CompetitionList = () => {
  const navigate = useNavigate();
  const [competitions, setCompetitions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadCompetitions();
  }, []);

  const loadCompetitions = async () => {
    try {
      const response = await competitionAPI.getAll();
      setCompetitions(response.data);
    } catch (error) {
      console.error('Error loading competitions:', error);
      setError('Fehler beim Laden der Wettkämpfe.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Sind Sie sicher, dass Sie diesen Wettkampf löschen möchten?')) {
      try {
        await competitionAPI.delete(id);
        setCompetitions(competitions.filter(comp => comp.id !== id));
      } catch (error) {
        console.error('Error deleting competition:', error);
        setError('Fehler beim Löschen des Wettkampfs.');
      }
    }
  };

  const getDaysUntilCompetition = (date) => {
    const competitionDate = moment(date);
    const today = moment();
    return competitionDate.diff(today, 'days');
  };

  const getStatusBadge = (date) => {
    const days = getDaysUntilCompetition(date);
    if (days < 0) {
      return <Badge bg="secondary">Vergangen</Badge>;
    } else if (days === 0) {
      return <Badge bg="danger">Heute!</Badge>;
    } else if (days <= 7) {
      return <Badge bg="warning">Diese Woche</Badge>;
    } else if (days <= 30) {
      return <Badge bg="info">Diesen Monat</Badge>;
    } else {
      return <Badge bg="success">Kommend</Badge>;
    }
  };

  if (loading) {
    return <div className="text-center">Lade Wettkämpfe...</div>;
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Meine Wettkämpfe</h2>
        <Button variant="primary" onClick={() => navigate('/competitions/new')}>
          Neuer Wettkampf
        </Button>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      {competitions.length === 0 ? (
        <Card>
          <Card.Body className="text-center py-5">
            <h4>Keine Wettkämpfe vorhanden</h4>
            <p className="text-muted">
              Erstellen Sie Ihren ersten Wettkampf, um mit der Trainingsplanung zu beginnen.
            </p>
            <Button variant="primary" onClick={() => navigate('/competitions/new')}>
              Ersten Wettkampf erstellen
            </Button>
          </Card.Body>
        </Card>
      ) : (
        <Row>
          {competitions.map((competition) => (
            <Col md={6} lg={4} key={competition.id} className="mb-4">
              <Card className="h-100">
                <Card.Header className="d-flex justify-content-between align-items-center">
                  <h5 className="mb-0">{competition.name}</h5>
                  {getStatusBadge(competition.date)}
                </Card.Header>
                <Card.Body>
                  <div className="mb-3">
                    <strong>Datum:</strong> {moment(competition.date).format('DD.MM.YYYY')}
                  </div>
                  <div className="mb-3">
                    <strong>Tage bis zum Wettkampf:</strong>{' '}
                    <span className={getDaysUntilCompetition(competition.date) <= 7 ? 'text-danger fw-bold' : ''}>
                      {getDaysUntilCompetition(competition.date)} Tage
                    </span>
                  </div>
                  {competition.description && (
                    <div className="mb-3">
                      <strong>Beschreibung:</strong>
                      <p className="text-muted small">{competition.description}</p>
                    </div>
                  )}
                </Card.Body>
                <Card.Footer>
                  <div className="d-flex justify-content-between">
                    <div>
                      <Button
                        variant="primary"
                        size="sm"
                        className="me-2"
                        onClick={() => navigate(`/competitions/${competition.id}/weeks`)}
                      >
                        Training
                      </Button>
                      <Button
                        variant="secondary"
                        size="sm"
                        className="me-2"
                        onClick={() => navigate(`/competitions/${competition.id}/upload`)}
                      >
                        Plan hochladen
                      </Button>
                    </div>
                    <div>
                      <Button
                        variant="outline-primary"
                        size="sm"
                        className="me-2"
                        onClick={() => navigate(`/competitions/${competition.id}/edit`)}
                      >
                        Bearbeiten
                      </Button>
                      <Button
                        variant="outline-danger"
                        size="sm"
                        onClick={() => handleDelete(competition.id)}
                      >
                        Löschen
                      </Button>
                    </div>
                  </div>
                </Card.Footer>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </div>
  );
};

export default CompetitionList;