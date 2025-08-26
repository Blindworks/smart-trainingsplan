package com.trainingsplan.service;

import com.garmin.fit.*;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.Training;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.service.TrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CompletedTrainingService {

    @Autowired
    private CompletedTrainingRepository completedTrainingRepository;
    
    @Autowired
    private TrainingService trainingService;

    public CompletedTraining uploadAndParseFitFile(MultipartFile file, LocalDate trainingDate) throws IOException {
        return uploadAndParseFitFile(file, trainingDate, null);
    }

    public CompletedTraining uploadAndParseFitFile(MultipartFile file, LocalDate trainingDate, Long trainingId) throws IOException {
        CompletedTraining training = new CompletedTraining();
        training.setTrainingDate(trainingDate);
        training.setOriginalFilename(file.getOriginalFilename());
        training.setUploadDate(LocalDateTime.now());

        try {
            // Echtes FIT-File Parsing
            parseFitFileReal(file.getBytes(), training);
        } catch (Exception e) {
            throw new IOException("Fehler beim Parsen der FIT-Datei: " + e.getMessage(), e);
        }

        CompletedTraining savedTraining = completedTrainingRepository.save(training);
        
        // If trainingId is provided, mark the planned training as completed
        if (trainingId != null) {
            Training plannedTraining = trainingService.findById(trainingId);
            if (plannedTraining != null) {
                trainingService.updateTrainingFeedback(trainingId, true, "completed");
            }
        }
        
        return savedTraining;
    }

    private void parseFitFileReal(byte[] fitData, CompletedTraining training) {
        Decode decode = new Decode();
        MesgBroadcaster mesgBroadcaster = new MesgBroadcaster(decode);
        
        FitDataCollector collector = new FitDataCollector(training);
        mesgBroadcaster.addListener(collector);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(fitData);
        
        if (!decode.checkFileIntegrity(inputStream)) {
            throw new RuntimeException("FIT-Datei ist beschädigt oder ungültig");
        }

        inputStream = new ByteArrayInputStream(fitData);
        try {
            decode.read(inputStream, mesgBroadcaster, mesgBroadcaster);
            collector.finalizeData();
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Lesen der FIT-Datei: " + e.getMessage(), e);
        }
    }
    
    private static class FitDataCollector implements MesgListener {
        private final CompletedTraining training;

        public FitDataCollector(CompletedTraining training) {
            this.training = training;
        }

        @Override
        public void onMesg(Mesg mesg) {
            String mesgName = mesg.getName();
            
            switch (mesgName) {
                case "activity":
                    handleActivity(mesg);
                    break;
                case "session":
                    handleSession(mesg);
                    break;
                case "device_info":
                    handleDeviceInfo(mesg);
                    break;
            }
        }
        
        private void handleActivity(Mesg mesg) {
            // Nur die direkten Werte aus der FIT-Datei nehmen
            setFieldValue(mesg, "total_distance", val -> training.setDistanceKm(val / 1000.0));
            setFieldValue(mesg, "total_timer_time", val -> training.setDurationSeconds(val.intValue()));
            setFieldValue(mesg, "total_moving_time", val -> training.setMovingTimeSeconds(val.intValue()));
            setFieldValue(mesg, "total_calories", val -> training.setCalories(val.intValue()));
            setFieldValue(mesg, "total_ascent", val -> training.setElevationGainM(val.intValue()));
            setFieldValue(mesg, "total_descent", val -> training.setElevationLossM(val.intValue()));
        }
        
        private void handleSession(Mesg mesg) {
            // Sport
            setFieldString(mesg, "sport", val -> training.setSport(val.toLowerCase()));
            setFieldString(mesg, "sub_sport", val -> training.setSubSport(val.toLowerCase()));
            
            // Direkte Werte aus Session - KEINE Berechnungen
            setFieldValue(mesg, "avg_speed", val -> training.setAverageSpeedKmh(val * 3.6));
            setFieldValue(mesg, "max_speed", val -> training.setMaxSpeedKmh(val * 3.6));
            setFieldValue(mesg, "avg_heart_rate", val -> training.setAverageHeartRate(val.intValue()));
            setFieldValue(mesg, "max_heart_rate", val -> training.setMaxHeartRate(val.intValue()));
            setFieldValue(mesg, "min_heart_rate", val -> training.setMinHeartRate(val.intValue()));
            setFieldValue(mesg, "avg_power", val -> training.setAveragePowerWatts(val.intValue()));
            setFieldValue(mesg, "max_power", val -> training.setMaxPowerWatts(val.intValue()));
            setFieldValue(mesg, "normalized_power", val -> training.setNormalizedPowerWatts(val.intValue()));
            setFieldValue(mesg, "avg_cadence", val -> training.setAverageCadence(val.intValue()));
            setFieldValue(mesg, "max_cadence", val -> training.setMaxCadence(val.intValue()));
            setFieldValue(mesg, "avg_temperature", val -> training.setTemperatureCelsius(val));
            
            // Pace direkt aus FIT (falls vorhanden)
            setFieldValue(mesg, "avg_running_cadence", val -> training.setAverageCadence(val.intValue()));
            setFieldValue(mesg, "max_running_cadence", val -> training.setMaxCadence(val.intValue()));
            
            // Fallbacks für fehlende Activity-Daten
            if (training.getDistanceKm() == null) {
                setFieldValue(mesg, "total_distance", val -> training.setDistanceKm(val / 1000.0));
            }
            if (training.getDurationSeconds() == null) {
                setFieldValue(mesg, "total_timer_time", val -> training.setDurationSeconds(val.intValue()));
            }
            if (training.getMovingTimeSeconds() == null) {
                setFieldValue(mesg, "total_moving_time", val -> training.setMovingTimeSeconds(val.intValue()));
            }
            if (training.getCalories() == null) {
                setFieldValue(mesg, "total_calories", val -> training.setCalories(val.intValue()));
            }
            if (training.getElevationGainM() == null) {
                setFieldValue(mesg, "total_ascent", val -> training.setElevationGainM(val.intValue()));
            }
            if (training.getElevationLossM() == null) {
                setFieldValue(mesg, "total_descent", val -> training.setElevationLossM(val.intValue()));
            }
        }
        
        private void handleDeviceInfo(Mesg mesg) {
            setFieldString(mesg, "manufacturer", val -> training.setDeviceManufacturer(val));
            setFieldString(mesg, "product", val -> training.setDeviceProduct(val));
            setFieldString(mesg, "serial_number", val -> training.setDeviceSerialNumber(val));
            setFieldString(mesg, "software_version", val -> training.setSoftwareVersion(val));
        }
        
        private void setFieldValue(Mesg mesg, String fieldName, java.util.function.Consumer<Double> setter) {
            Field field = mesg.getField(fieldName);
            if (field != null && field.getValue() != null) {
                Double value = field.getDoubleValue();
                if (value != null) {
                    setter.accept(value);
                }
            }
        }
        
        private void setFieldString(Mesg mesg, String fieldName, java.util.function.Consumer<String> setter) {
            Field field = mesg.getField(fieldName);
            if (field != null && field.getValue() != null) {
                String value = field.getValue().toString();
                if (value != null && !value.isEmpty()) {
                    setter.accept(value);
                }
            }
        }
        
        public void finalizeData() {
            // NUR Pace berechnen falls nicht direkt vorhanden UND beide Werte existieren
            if (training.getAveragePaceSecondsPerKm() == null &&
                training.getDistanceKm() != null && training.getDurationSeconds() != null &&
                training.getDurationSeconds() > 0 && training.getDistanceKm() > 0) {
                double paceSecondsPerKm = training.getDurationSeconds() / training.getDistanceKm();
                training.setAveragePaceSecondsPerKm((int) paceSecondsPerKm);
            }
        }
    }

    public List<CompletedTraining> getCompletedTrainingsByDate(LocalDate date) {
        return completedTrainingRepository.findByTrainingDateOrderByUploadDateDesc(date);
    }

    public List<CompletedTraining> getCompletedTrainingsBetweenDates(LocalDate startDate, LocalDate endDate) {
        return completedTrainingRepository.findByTrainingDateBetweenOrderByTrainingDate(startDate, endDate);
    }
}