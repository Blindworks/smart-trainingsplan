import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Form, Button, Alert, Card, Row, Col } from 'react-bootstrap';
import DatePicker from 'react-datepicker';
import { competitionAPI } from '../services/api';
import 'react-datepicker/dist/react-datepicker.css';

const CompetitionForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = !!id;

  const [formData, setFormData] = useState({
    name: '',
    date: new Date(),
    description: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isEdit) {
      loadCompetition();
    }
  }, [id, isEdit]);

  const loadCompetition = async () => {
    try {
      const response = await competitionAPI.getById(id);
      const competition = response.data;
      setFormData({
        name: competition.name,
        date: new Date(competition.date),
        description: competition.description || ''
      });
    } catch (error) {
      console.error('Error loading competition:', error);
      setError('Fehler beim Laden des Wettkampfs.');
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleDateChange = (date) => {
    setFormData(prev => ({
      ...prev,
      date: date
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!formData.name || !formData.date) {
      setError('Name und Datum sind erforderlich.');
      return;
    }

    if (formData.date <= new Date()) {
      setError('Das Wettkampfdatum muss in der Zukunft liegen.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const competitionData = {
        name: formData.name,
        date: formData.date.toISOString().split('T')[0],
        description: formData.description
      };

      let response;
      if (isEdit) {
        response = await competitionAPI.update(id, competitionData);
      } else {
        response = await competitionAPI.create(competitionData);
        
        const competitionId = response.data.id;
        await competitionAPI.generateWeeks(competitionId);
      }

      navigate('/competitions');

    } catch (error) {
      console.error('Error saving competition:', error);
      setError('Fehler beim Speichern des Wettkampfs.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Row className="justify-content-center">
      <Col md={8}>
        <Card>
          <Card.Header>
            <h3>{isEdit ? 'Wettkampf bearbeiten' : 'Neuer Wettkampf'}</h3>
          </Card.Header>
          <Card.Body>
            {error && <Alert variant="danger">{error}</Alert>}

            <Form onSubmit={handleSubmit}>
              <Form.Group className="mb-3">
                <Form.Label>Name des Wettkampfs *</Form.Label>
                <Form.Control
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  placeholder="z.B. Berlin Marathon 2024"
                  required
                />
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Wettkampfdatum *</Form.Label>
                <div>
                  <DatePicker
                    selected={formData.date}
                    onChange={handleDateChange}
                    dateFormat="dd.MM.yyyy"
                    minDate={new Date()}
                    className="form-control"
                    placeholderText="Wählen Sie ein Datum"
                    required
                  />
                </div>
                <Form.Text className="text-muted">
                  Das Datum muss in der Zukunft liegen
                </Form.Text>
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Beschreibung</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={3}
                  name="description"
                  value={formData.description}
                  onChange={handleInputChange}
                  placeholder="Zusätzliche Informationen zum Wettkampf (optional)"
                />
              </Form.Group>

              <div className="d-flex justify-content-between">
                <Button
                  variant="secondary"
                  onClick={() => navigate('/competitions')}
                  disabled={loading}
                >
                  Abbrechen
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  disabled={loading}
                >
                  {loading ? 'Speichere...' : isEdit ? 'Aktualisieren' : 'Erstellen'}
                </Button>
              </div>
            </Form>
          </Card.Body>
        </Card>

        {!isEdit && (
          <Alert variant="info" className="mt-3">
            <Alert.Heading>Was passiert nach dem Erstellen?</Alert.Heading>
            <p className="mb-0">
              Nach dem Erstellen des Wettkampfs werden automatisch Trainingswochen bis zum Wettkampfdatum generiert. 
              Sie können dann Trainingspläne hochladen und die wöchentlichen Trainings verwalten.
            </p>
          </Alert>
        )}
      </Col>
    </Row>
  );
};

export default CompetitionForm;