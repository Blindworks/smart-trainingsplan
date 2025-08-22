const express = require('express');
const cors = require('cors');
const app = express();
const PORT = 8080;

// Middleware
app.use(cors());
app.use(express.json());

// Mock data
let competitions = [
  {
    id: 1,
    name: "Berlin Marathon 2024",
    targetDate: "2024-09-29",
    location: "Berlin",
    description: "42.195km Marathonlauf durch Berlin",
    createdAt: "2024-01-15T10:00:00Z",
    updatedAt: "2024-01-15T10:00:00Z"
  },
  {
    id: 2,
    name: "Hamburg Halbmarathon",
    targetDate: "2024-06-15",
    location: "Hamburg",
    description: "21.1km Halbmarathon",
    createdAt: "2024-01-20T10:00:00Z",
    updatedAt: "2024-01-20T10:00:00Z"
  }
];

let trainings = [
  {
    id: 1,
    name: "Intervalltraining",
    description: "5x1000m mit 3min Pause",
    date: "2024-08-22",
    type: "speed",
    intensity: "high",
    startTime: "18:00",
    duration: 60,
    completed: false,
    rating: null,
    feedback: null,
    competitionId: 1
  },
  {
    id: 2,
    name: "Regenerationslauf",
    description: "Lockerer 30min Lauf",
    date: "2024-08-23",
    type: "recovery",
    intensity: "low",
    startTime: "07:00",
    duration: 30,
    completed: true,
    rating: 4,
    feedback: "Sehr entspannend",
    competitionId: 1
  }
];

let trainingPlans = [];
let completedTrainings = [];

// Competition endpoints
app.get('/api/competitions', (req, res) => {
  res.json(competitions);
});

app.get('/api/competitions/:id', (req, res) => {
  const competition = competitions.find(c => c.id === parseInt(req.params.id));
  if (!competition) {
    return res.status(404).json({ message: 'Competition not found' });
  }
  res.json(competition);
});

app.post('/api/competitions', (req, res) => {
  const newCompetition = {
    id: competitions.length + 1,
    ...req.body,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };
  competitions.push(newCompetition);
  res.status(201).json(newCompetition);
});

app.put('/api/competitions/:id', (req, res) => {
  const index = competitions.findIndex(c => c.id === parseInt(req.params.id));
  if (index === -1) {
    return res.status(404).json({ message: 'Competition not found' });
  }
  competitions[index] = {
    ...competitions[index],
    ...req.body,
    updatedAt: new Date().toISOString()
  };
  res.json(competitions[index]);
});

app.delete('/api/competitions/:id', (req, res) => {
  const index = competitions.findIndex(c => c.id === parseInt(req.params.id));
  if (index === -1) {
    return res.status(404).json({ message: 'Competition not found' });
  }
  competitions.splice(index, 1);
  res.status(204).send();
});

// Training overview endpoint
app.post('/api/trainings/overview', (req, res) => {
  const { competitionIds, startDate, endDate } = req.body;
  
  // Filter trainings by date range and competition IDs
  const filteredTrainings = trainings.filter(training => {
    const trainingDate = new Date(training.date);
    const start = new Date(startDate);
    const end = new Date(endDate);
    
    return trainingDate >= start && 
           trainingDate <= end && 
           (!competitionIds || competitionIds.length === 0 || competitionIds.includes(training.competitionId));
  });
  
  res.json(filteredTrainings);
});

// Training endpoints
app.get('/api/trainings', (req, res) => {
  const { date } = req.query;
  if (date) {
    const filtered = trainings.filter(t => t.date === date);
    return res.json(filtered);
  }
  res.json(trainings);
});

app.post('/api/trainings/:id/feedback', (req, res) => {
  const index = trainings.findIndex(t => t.id === parseInt(req.params.id));
  if (index === -1) {
    return res.status(404).json({ message: 'Training not found' });
  }
  
  trainings[index] = {
    ...trainings[index],
    ...req.body
  };
  
  res.json(trainings[index]);
});

// Training plan upload
app.post('/api/competitions/:id/training-plans', (req, res) => {
  const competitionId = parseInt(req.params.id);
  const trainingPlan = {
    id: trainingPlans.length + 1,
    competitionId,
    trainings: req.body.trainings || [],
    uploadedAt: new Date().toISOString()
  };
  
  // Add trainings to the trainings array
  req.body.trainings?.forEach(training => {
    trainings.push({
      id: trainings.length + 1,
      ...training,
      competitionId,
      completed: false,
      rating: null,
      feedback: null
    });
  });
  
  trainingPlans.push(trainingPlan);
  res.status(201).json(trainingPlan);
});

// FIT file upload
app.post('/api/fit-files', (req, res) => {
  // Mock FIT file processing
  res.json({
    success: true,
    message: 'FIT file processed successfully',
    data: {
      distance: 10.5,
      duration: 3600,
      avgHeartRate: 150,
      calories: 750
    }
  });
});

// Completed trainings
app.get('/api/completed-trainings', (req, res) => {
  const { date } = req.query;
  if (date) {
    const filtered = completedTrainings.filter(ct => ct.trainingDate === date);
    return res.json(filtered);
  }
  res.json(completedTrainings);
});

// Health check
app.get('/api/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

app.listen(PORT, () => {
  console.log(`🚀 Mock Backend Server running on http://localhost:${PORT}`);
  console.log(`📊 Available endpoints:`);
  console.log(`   GET  /api/competitions`);
  console.log(`   POST /api/competitions`);
  console.log(`   POST /api/trainings/overview`);
  console.log(`   GET  /api/trainings?date=YYYY-MM-DD`);
  console.log(`   POST /api/trainings/:id/feedback`);
  console.log(`   POST /api/competitions/:id/training-plans`);
  console.log(`   POST /api/fit-files`);
  console.log(`   GET  /api/health`);
});