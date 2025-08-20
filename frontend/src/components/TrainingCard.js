import React, { useState } from 'react';
import { Card, Badge, Button, Modal, Form, Alert } from 'react-bootstrap';
import moment from 'moment';

const TrainingCard = ({ training, onFeedback }) => {
  const [showFeedbackModal, setShowFeedbackModal] = useState(false);
  const [feedbackData, setFeedbackData] = useState({
    isCompleted: false,
    completionStatus: ''
  });

  const getIntensityColor = (intensity) => {
    switch (intensity?.toLowerCase()) {
      case 'high': return 'danger';
      case 'medium': return 'warning';
      case 'low': return 'success';
      default: return 'secondary';
    }
  };

  const getIntensityClass = (intensity) => {
    switch (intensity?.toLowerCase()) {
      case 'high': return 'intensity-high';
      case 'medium': return 'intensity-medium';
      case 'low': return 'intensity-low';
      default: return '';
    }
  };

  const handleFeedbackSubmit = () => {
    onFeedback(training.id, feedbackData.isCompleted, feedbackData.completionStatus);
    setShowFeedbackModal(false);
    setFeedbackData({ isCompleted: false, completionStatus: '' });
  };

  const openFeedbackModal = () => {
    setFeedbackData({
      isCompleted: training.isCompleted || false,
      completionStatus: training.completionStatus || ''
    });
    setShowFeedbackModal(true);
  };

  return (
    <>
      <Card className={`training-card h-100 ${getIntensityClass(training.intensityLevel)}`}>
        <Card.Header className="d-flex justify-content-between align-items-center pb-2">
          <h6 className="mb-0">{training.name}</h6>
          <div>
            <Badge bg={getIntensityColor(training.intensityLevel)} className="me-2">
              {training.intensityLevel || 'Normal'}
            </Badge>
            {training.isCompleted && (
              <Badge bg="success">✓</Badge>
            )}
          </div>
        </Card.Header>
        <Card.Body>
          {training.description && (
            <p className="text-muted small mb-2">{training.description}</p>
          )}
          
          <div className="small mb-2">
            {training.trainingType && (
              <div><strong>Typ:</strong> {training.trainingType}</div>
            )}
            {training.startTime && (
              <div><strong>Zeit:</strong> {training.startTime}</div>
            )}
            {training.durationMinutes && (
              <div><strong>Dauer:</strong> {training.durationMinutes} Min</div>
            )}
          </div>

          {training.isCompleted && training.completionStatus && (
            <Alert variant={training.completionStatus === 'completed' ? 'success' : 'warning'} className="py-2">
              <small>
                <strong>Status:</strong> {
                  training.completionStatus === 'completed' ? 'Vollständig absolviert' :
                  training.completionStatus === 'partial' ? 'Teilweise absolviert' :
                  training.completionStatus === 'skipped' ? 'Übersprungen' :
                  training.completionStatus
                }
              </small>
            </Alert>
          )}
        </Card.Body>
        <Card.Footer className="bg-transparent border-top-0">
          <Button
            variant={training.isCompleted ? "outline-primary" : "primary"}
            size="sm"
            onClick={openFeedbackModal}
            className="w-100"
          >
            {training.isCompleted ? 'Feedback bearbeiten' : 'Training abschließen'}
          </Button>
        </Card.Footer>
      </Card>

      <Modal show={showFeedbackModal} onHide={() => setShowFeedbackModal(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Training Feedback: {training.name}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form>
            <Form.Group className="mb-3">
              <Form.Label>Haben Sie das Training absolviert?</Form.Label>
              <div>
                <Form.Check
                  type="radio"
                  id="completed-yes"
                  name="isCompleted"
                  label="Ja, vollständig absolviert"
                  checked={feedbackData.isCompleted === true}
                  onChange={() => setFeedbackData({...feedbackData, isCompleted: true, completionStatus: 'completed'})}
                />
                <Form.Check
                  type="radio"
                  id="completed-partial"
                  name="isCompleted"
                  label="Teilweise absolviert"
                  checked={feedbackData.completionStatus === 'partial'}
                  onChange={() => setFeedbackData({...feedbackData, isCompleted: true, completionStatus: 'partial'})}
                />
                <Form.Check
                  type="radio"
                  id="completed-no"
                  name="isCompleted"
                  label="Nein, nicht absolviert"
                  checked={feedbackData.isCompleted === false}
                  onChange={() => setFeedbackData({...feedbackData, isCompleted: false, completionStatus: 'skipped'})}
                />
              </div>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Zusätzliche Notizen (optional)</Form.Label>
              <Form.Control
                as="textarea"
                rows={3}
                value={feedbackData.completionStatus}
                onChange={(e) => setFeedbackData({...feedbackData, completionStatus: e.target.value})}
                placeholder="Wie lief das Training? Gab es Probleme?"
              />
            </Form.Group>
          </Form>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowFeedbackModal(false)}>
            Abbrechen
          </Button>
          <Button variant="primary" onClick={handleFeedbackSubmit}>
            Feedback speichern
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
};

export default TrainingCard;