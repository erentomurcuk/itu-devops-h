package Metrics;

import io.prometheus.client.*;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.hotspot.DefaultExports;

import java.io.IOException;
import java.io.StringWriter;

public class PrometheusMetrics {
    final private CollectorRegistry registry = CollectorRegistry.defaultRegistry;

    public PrometheusMetrics() {
        DefaultExports.initialize();
    }

    public String metrics() throws IOException {
        final StringWriter writer = new StringWriter();
        TextFormat.write004(writer, registry.metricFamilySamples());
        return writer.toString();
    }
}
