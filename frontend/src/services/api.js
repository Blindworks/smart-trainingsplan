import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const competitionAPI = {
  getAll: () => api.get('/competitions'),
  getById: (id) => api.get(`/competitions/${id}`),
  create: (competition) => api.post('/competitions', competition),
  update: (id, competition) => api.put(`/competitions/${id}`, competition),
  delete: (id) => api.delete(`/competitions/${id}`),
  generateWeeks: (id) => api.post(`/competitions/${id}/generate-weeks`)
};

export const trainingPlanAPI = {
  getAll: () => api.get('/training-plans'),
  getById: (id) => api.get(`/training-plans/${id}`),
  getByCompetition: (competitionId) => api.get(`/training-plans/competition/${competitionId}`),
  create: (trainingPlan) => api.post('/training-plans', trainingPlan),
  update: (id, trainingPlan) => api.put(`/training-plans/${id}`, trainingPlan),
  delete: (id) => api.delete(`/training-plans/${id}`),
  upload: (formData) => api.post('/training-plans/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
};

export const trainingAPI = {
  getAll: () => api.get('/trainings'),
  getById: (id) => api.get(`/trainings/${id}`),
  getByWeek: (weekId) => api.get(`/trainings/week/${weekId}`),
  getByDate: (date) => api.get(`/trainings/date/${date}`),
  getByCompetitionAndDate: (competitionId, date) => api.get(`/trainings/competition/${competitionId}/date/${date}`),
  getMixed: (competitionId, planIds, date) => api.get(`/trainings/competition/${competitionId}/mixed`, {
    params: { planIds, date }
  }),
  getOverview: (competitionIds, startDate, endDate) => api.post('/trainings/overview', {
    competitionIds,
    startDate,
    endDate
  }),
  create: (training) => api.post('/trainings', training),
  update: (id, training) => api.put(`/trainings/${id}`, training),
  updateFeedback: (id, feedback) => api.put(`/trainings/${id}/feedback`, feedback),
  delete: (id) => api.delete(`/trainings/${id}`)
};

export default api;