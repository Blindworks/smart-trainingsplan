import React, { useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Form, Button, Alert, Card, Row, Col } from 'react-bootstrap';
import { useDropzone } from 'react-dropzone';
import { trainingPlanAPI } from '../services/api';

const TrainingPlanUpload = () => {
  const { id: competitionId } = useParams();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    file: null
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const onDrop = useCallback((acceptedFiles) => {
    const file = acceptedFiles[0];
    if (file && file.type === 'application/json') {
      setFormData(prev => ({ ...prev, file }));
      setError('');
    } else {
      setError('Bitte wählen Sie eine gültige JSON-Datei aus.');
    }
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'application/json': ['.json']
    },
    multiple: false
  });

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!formData.name || !formData.file) {
      setError('Name und Datei sind erforderlich.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const uploadData = new FormData();
      uploadData.append('file', formData.file);
      uploadData.append('name', formData.name);
      uploadData.append('description', formData.description);
      uploadData.append('competitionId', competitionId);

      await trainingPlanAPI.upload(uploadData);
      setSuccess('Trainingsplan erfolgreich hochgeladen!');
      
      setTimeout(() => {
        navigate(`/competitions/${competitionId}/weeks`);
      }, 2000);

    } catch (error) {
      console.error('Upload error:', error);
      setError('Fehler beim Hochladen des Trainingsplans.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Row className="justify-content-center">
      <Col md={8}>
        <Card>
          <Card.Header>
            <h3>Trainingsplan hochladen</h3>
          </Card.Header>
          <Card.Body>
            {error && <Alert variant="danger">{error}</Alert>}
            {success && <Alert variant="success">{success}</Alert>}

            <Form onSubmit={handleSubmit}>
              <Form.Group className="mb-3">
                <Form.Label>Name des Trainingsplans</Form.Label>
                <Form.Control
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  placeholder="Geben Sie einen Namen ein"
                  required
                />
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>Beschreibung (optional)</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={3}
                  name="description"
                  value={formData.description}
                  onChange={handleInputChange}
                  placeholder="Beschreibung des Trainingsplans"
                />
              </Form.Group>

              <Form.Group className="mb-3">
                <Form.Label>JSON-Datei</Form.Label>
                <div
                  {...getRootProps()}
                  className={`border-2 border-dashed rounded p-4 text-center ${
                    isDragActive ? 'border-primary' : 'border-secondary'
                  }`}
                  style={{ cursor: 'pointer' }}
                >
                  <input {...getInputProps()} />
                  {formData.file ? (
                    <div>
                      <p className="mb-0 text-success">
                        <strong>Datei ausgewählt:</strong> {formData.file.name}
                      </p>
                      <small className="text-muted">
                        Klicken Sie hier oder ziehen Sie eine andere Datei hierher, um sie zu ersetzen
                      </small>
                    </div>
                  ) : (
                    <div>
                      <p className="mb-0">
                        {isDragActive
                          ? 'Lassen Sie die JSON-Datei hier fallen...'
                          : 'Klicken Sie hier oder ziehen Sie eine JSON-Datei hierher'}
                      </p>
                      <small className="text-muted">Nur JSON-Dateien sind erlaubt</small>
                    </div>
                  )}
                </div>
              </Form.Group>

              <div className="d-flex justify-content-between">
                <Button
                  variant="secondary"
                  onClick={() => navigate(`/competitions/${competitionId}/weeks`)}
                >
                  Zurück
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  disabled={loading || !formData.name || !formData.file}
                >
                  {loading ? 'Uploading...' : 'Hochladen'}
                </Button>
              </div>
            </Form>
          </Card.Body>
        </Card>

        <Card className="mt-4">
          <Card.Header>
            <h5>JSON-Format Beispiel</h5>
          </Card.Header>
          <Card.Body>
            <div>
              <h6>Format 1: Einfache Trainingsliste</h6>
              <pre className="bg-light p-3 rounded small">
                {JSON.stringify({
                  "trainings": [
                    {
                      "name": "Intervalltraining",
                      "description": "5x1000m Intervalle mit 3min Pause",
                      "date": "2024-01-15",
                      "type": "speed",
                      "intensity": "high",
                      "startTime": "18:00",
                      "duration": 90
                    }
                  ]
                }, null, 2)}
              </pre>

              <h6>Format 2: Marathon-Trainingsplan (Wochenstruktur)</h6>
              <pre className="bg-light p-3 rounded small">
                {JSON.stringify({
                  "marathon_plan": {
                    "title": "Marathon-Trainingsplan",
                    "weeks": [
                      {
                        "week": 1,
                        "schedule": {
                          "monday": {
                            "workout": "12 km langsamer DL, GA1",
                            "intensity": "70%"
                          },
                          "tuesday": {
                            "workout": "10 km lockerer DL, GA2",
                            "intensity": "80%"
                          }
                        }
                      }
                    ]
                  }
                }, null, 2)}
              </pre>
              
              <div className="mt-2">
                <small className="text-muted">
                  <strong>Hinweis:</strong> Beide Formate werden unterstützt. 
                  Das Marathon-Format erstellt automatisch Trainings basierend auf Wochentagen.
                </small>
              </div>
            </div>
          </Card.Body>
        </Card>
      </Col>
    </Row>
  );
};

export default TrainingPlanUpload;