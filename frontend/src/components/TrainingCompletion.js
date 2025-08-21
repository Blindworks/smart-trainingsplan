import React, { useState, useEffect } from 'react';
import { Card, ProgressBar, Alert, Row, Col, Badge, ListGroup } from 'react-bootstrap';
import axios from 'axios';

const TrainingCompletion = () => {
    const [todayCompletion, setTodayCompletion] = useState(null);
    const [weekCompletion, setWeekCompletion] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchTodayCompletion();
        fetchCurrentWeekCompletion();
    }, []);

    const fetchTodayCompletion = async () => {
        try {
            const response = await axios.get('http://localhost:8080/api/training-completion/today');
            setTodayCompletion(response.data);
        } catch (err) {
            console.error('Error fetching today\'s completion:', err);
            setError('Fehler beim Laden der heutigen Trainings');
        }
    };

    const fetchCurrentWeekCompletion = async () => {
        try {
            const response = await axios.get('http://localhost:8080/api/training-completion/current-week');
            setWeekCompletion(response.data);
            setLoading(false);
        } catch (err) {
            console.error('Error fetching week completion:', err);
            setError('Fehler beim Laden der Wochenübersicht');
            setLoading(false);
        }
    };

    const formatDate = (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('de-DE', {
            weekday: 'short',
            day: '2-digit',
            month: '2-digit'
        });
    };

    const getCompletionVariant = (percentage) => {
        if (percentage >= 100) return 'success';
        if (percentage >= 75) return 'info';
        if (percentage >= 50) return 'warning';
        return 'danger';
    };

    if (loading) return <Alert variant="info">Lade Trainingsdaten...</Alert>;
    if (error) return <Alert variant="danger">{error}</Alert>;

    return (
        <div>
            <h2>Training Completion Übersicht</h2>
            
            {/* Today's Completion */}
            {todayCompletion && (
                <Card className="mb-4">
                    <Card.Header>
                        <h5>Heute ({formatDate(todayCompletion.date)})</h5>
                    </Card.Header>
                    <Card.Body>
                        <Row>
                            <Col md={6}>
                                <div className="mb-3">
                                    <strong>Fortschritt: {todayCompletion.completionPercentage.toFixed(1)}%</strong>
                                    <ProgressBar 
                                        now={todayCompletion.completionPercentage} 
                                        variant={getCompletionVariant(todayCompletion.completionPercentage)}
                                        className="mt-2"
                                    />
                                </div>
                                <p>
                                    <Badge bg="primary" className="me-2">
                                        {todayCompletion.plannedTrainingsCount} geplant
                                    </Badge>
                                    <Badge bg="success">
                                        {todayCompletion.completedTrainingsCount} absolviert
                                    </Badge>
                                </p>
                            </Col>
                            <Col md={6}>
                                <Row>
                                    <Col>
                                        <strong>Geplante Trainings:</strong>
                                        <ListGroup variant="flush" className="mt-2">
                                            {todayCompletion.plannedTrainingNames.map((name, index) => (
                                                <ListGroup.Item key={index} className="py-1 px-0 border-0">
                                                    <small>{name}</small>
                                                </ListGroup.Item>
                                            ))}
                                            {todayCompletion.plannedTrainingNames.length === 0 && (
                                                <small className="text-muted">Keine geplanten Trainings</small>
                                            )}
                                        </ListGroup>
                                    </Col>
                                    <Col>
                                        <strong>Absolvierte Trainings:</strong>
                                        <ListGroup variant="flush" className="mt-2">
                                            {todayCompletion.completedTrainingSports.map((sport, index) => (
                                                <ListGroup.Item key={index} className="py-1 px-0 border-0">
                                                    <small>{sport}</small>
                                                </ListGroup.Item>
                                            ))}
                                            {todayCompletion.completedTrainingSports.length === 0 && (
                                                <small className="text-muted">Noch keine Trainings absolviert</small>
                                            )}
                                        </ListGroup>
                                    </Col>
                                </Row>
                            </Col>
                        </Row>
                    </Card.Body>
                </Card>
            )}

            {/* Weekly Overview */}
            <Card>
                <Card.Header>
                    <h5>Wochenübersicht</h5>
                </Card.Header>
                <Card.Body>
                    {weekCompletion.map((day, index) => (
                        <div key={index} className="mb-3">
                            <div className="d-flex justify-content-between align-items-center mb-1">
                                <span><strong>{formatDate(day.date)}</strong></span>
                                <span>{day.completionPercentage.toFixed(1)}%</span>
                            </div>
                            <ProgressBar 
                                now={day.completionPercentage} 
                                variant={getCompletionVariant(day.completionPercentage)}
                            />
                            <div className="mt-1">
                                <small className="text-muted">
                                    {day.plannedTrainingsCount} geplant, {day.completedTrainingsCount} absolviert
                                </small>
                            </div>
                        </div>
                    ))}
                </Card.Body>
            </Card>
        </div>
    );
};

export default TrainingCompletion;