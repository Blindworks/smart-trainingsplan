package com.trainingsplan.service;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class GpxParsingServiceElevationTest {

    private static final String GPX = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
          <trk><name>t</name><type>cycling</type><trkseg>
            <trkpt lat="50.1780" lon="8.7400"><ele>100.0</ele><time>2026-06-01T08:00:00Z</time></trkpt>
            <trkpt lat="50.1783" lon="8.7400"><ele>110.0</ele><time>2026-06-01T08:00:05Z</time></trkpt>
            <trkpt lat="50.1786" lon="8.7400"><ele>125.5</ele><time>2026-06-01T08:00:10Z</time></trkpt>
          </trkseg></trk>
        </gpx>
        """;

    @Test
    void parse_populatesPerPointElevation() throws Exception {
        GpxParsingService svc = new GpxParsingService();
        ParsedActivityData data = svc.parse(GPX.getBytes(StandardCharsets.UTF_8));

        assertNotNull(data.elevations);
        assertEquals(3, data.elevations.size());
        assertEquals(100.0, data.elevations.get(0), 1e-9);
        assertEquals(110.0, data.elevations.get(1), 1e-9);
        assertEquals(125.5, data.elevations.get(2), 1e-9);
        // sanity: existing streams still aligned
        assertEquals(3, data.latLngPoints.size());
        assertEquals(3, data.timeSeconds.size());
        assertEquals(0, data.timeSeconds.get(0));
        assertEquals(10, data.timeSeconds.get(2));
    }

    @Test
    void parse_missingElevation_yieldsNullEntry() throws Exception {
        String noEle = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="test" xmlns="http://www.topografix.com/GPX/1/1">
              <trk><trkseg>
                <trkpt lat="50.1780" lon="8.7400"><time>2026-06-01T08:00:00Z</time></trkpt>
              </trkseg></trk>
            </gpx>
            """;
        ParsedActivityData data = new GpxParsingService().parse(noEle.getBytes(StandardCharsets.UTF_8));
        assertEquals(1, data.elevations.size());
        assertNull(data.elevations.get(0));
    }
}
