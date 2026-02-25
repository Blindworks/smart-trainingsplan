package com.trainingsplan.dto;

import java.time.LocalDate;
import java.util.List;

public class DashboardDto {

    private final double strain21;
    private final int readinessScore;
    private final String readinessRecommendation;
    private final LoadStatusDto loadStatus;
    private final List<LoadTrendPointDto> loadTrend;
    private final List<EfTrendPointDto> efTrend;
    private final List<DriftTrendPointDto> driftTrend;
    private final LastRunDto lastRun;

    public DashboardDto(
            double strain21,
            int readinessScore,
            String readinessRecommendation,
            LoadStatusDto loadStatus,
            List<LoadTrendPointDto> loadTrend,
            List<EfTrendPointDto> efTrend,
            List<DriftTrendPointDto> driftTrend,
            LastRunDto lastRun
    ) {
        this.strain21 = strain21;
        this.readinessScore = readinessScore;
        this.readinessRecommendation = readinessRecommendation;
        this.loadStatus = loadStatus;
        this.loadTrend = loadTrend;
        this.efTrend = efTrend;
        this.driftTrend = driftTrend;
        this.lastRun = lastRun;
    }

    public double getStrain21() {
        return strain21;
    }

    public int getReadinessScore() {
        return readinessScore;
    }

    public String getReadinessRecommendation() {
        return readinessRecommendation;
    }

    public LoadStatusDto getLoadStatus() {
        return loadStatus;
    }

    public List<LoadTrendPointDto> getLoadTrend() {
        return loadTrend;
    }

    public List<EfTrendPointDto> getEfTrend() {
        return efTrend;
    }

    public List<DriftTrendPointDto> getDriftTrend() {
        return driftTrend;
    }

    public LastRunDto getLastRun() {
        return lastRun;
    }

    public static class LoadStatusDto {
        private final double acwr;
        private final String flag;

        public LoadStatusDto(double acwr, String flag) {
            this.acwr = acwr;
            this.flag = flag;
        }

        public double getAcwr() {
            return acwr;
        }

        public String getFlag() {
            return flag;
        }
    }

    public static class LoadTrendPointDto {
        private final LocalDate date;
        private final double strain21;

        public LoadTrendPointDto(LocalDate date, double strain21) {
            this.date = date;
            this.strain21 = strain21;
        }

        public LocalDate getDate() {
            return date;
        }

        public double getStrain21() {
            return strain21;
        }
    }

    public static class EfTrendPointDto {
        private final LocalDate date;
        private final double ef;

        public EfTrendPointDto(LocalDate date, double ef) {
            this.date = date;
            this.ef = ef;
        }

        public LocalDate getDate() {
            return date;
        }

        public double getEf() {
            return ef;
        }
    }

    public static class DriftTrendPointDto {
        private final LocalDate date;
        private final double driftPct;

        public DriftTrendPointDto(LocalDate date, double driftPct) {
            this.date = date;
            this.driftPct = driftPct;
        }

        public LocalDate getDate() {
            return date;
        }

        public double getDriftPct() {
            return driftPct;
        }
    }

    public static class LastRunDto {
        private final LocalDate date;
        private final double strain21;
        private final double driftPct;
        private final double z4Min;
        private final double z5Min;
        private final List<String> coachBullets;

        public LastRunDto(
                LocalDate date,
                double strain21,
                double driftPct,
                double z4Min,
                double z5Min,
                List<String> coachBullets
        ) {
            this.date = date;
            this.strain21 = strain21;
            this.driftPct = driftPct;
            this.z4Min = z4Min;
            this.z5Min = z5Min;
            this.coachBullets = coachBullets;
        }

        public LocalDate getDate() {
            return date;
        }

        public double getStrain21() {
            return strain21;
        }

        public double getDriftPct() {
            return driftPct;
        }

        public double getZ4Min() {
            return z4Min;
        }

        public double getZ5Min() {
            return z5Min;
        }

        public List<String> getCoachBullets() {
            return coachBullets;
        }
    }
}
