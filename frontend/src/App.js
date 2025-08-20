import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Container } from 'react-bootstrap';
import Navigation from './components/Navigation';
import CompetitionList from './components/CompetitionList';
import CompetitionForm from './components/CompetitionForm';
import TrainingPlanUpload from './components/TrainingPlanUpload';
import TrainingWeekView from './components/TrainingWeekView';
import TrainingPlanOverview from './components/TrainingPlanOverview';
import './App.css';

function App() {
  return (
    <Router>
      <div className="App">
        <Navigation />
        <Container className="mt-4">
          <Routes>
            <Route path="/" element={<CompetitionList />} />
            <Route path="/competitions" element={<CompetitionList />} />
            <Route path="/overview" element={<TrainingPlanOverview />} />
            <Route path="/competitions/new" element={<CompetitionForm />} />
            <Route path="/competitions/:id/edit" element={<CompetitionForm />} />
            <Route path="/competitions/:id/upload" element={<TrainingPlanUpload />} />
            <Route path="/competitions/:id/weeks" element={<TrainingWeekView />} />
          </Routes>
        </Container>
      </div>
    </Router>
  );
}

export default App;