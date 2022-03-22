package Metrics;

import io.prometheus.client.*;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.hotspot.DefaultExports;

import java.io.IOException;
import java.io.StringWriter;

public class PrometheusMetrics {
    final private CollectorRegistry registry = CollectorRegistry.defaultRegistry;

    static final private String prefix = "minitwit_";

    final static Histogram requestLatency = Histogram.build()
            .labelNames("endpoint_type", "status_code")
            .name(prefix + "requests_time_seconds")
            .help("Request processing time")
            .register();
    final static Counter registrations = Counter.build()
            .labelNames("endpoint_type")
            .name(prefix + "registrations_total")
            .help("Total registrations.")
            .register();
    final static Counter follows = Counter.build()
            .labelNames("endpoint_type")
            .name(prefix + "follows_total")
            .help("Total follows.")
            .register();
    final static Counter unfollows = Counter.build()
            .labelNames("endpoint_type")
            .name(prefix + "unfollows_total")
            .help("Total unfollows.")
            .register();
    final static Counter messages = Counter.build()
            .labelNames("endpoint_type")
            .name(prefix + "messages_total")
            .help("Total messages.")
            .register();
    final static Counter signins = Counter.build()
            .labelNames("endpoint_type")
            .name(prefix + "signins_total")
            .help("Total signins.")
            .register();
    final static Counter signouts = Counter.build()
            .labelNames("endpoint_type")
            .name(prefix + "signouts_total")
            .help("Total signouts.")
            .register();

    public PrometheusMetrics() {
        DefaultExports.initialize();
    }

    public PrometheusMetrics incrementRegistrations(String type) {
        registrations.labels(type).inc();
        return this;
    }
    public PrometheusMetrics incrementFollows(String type) {
        follows.labels(type).inc();
        return this;
    }
    public PrometheusMetrics incrementUnfollows(String type) {
        unfollows.labels(type).inc();
        return this;
    }
    public PrometheusMetrics incrementMessages(String type) {
        messages.labels(type).inc();
        return this;
    }
    public PrometheusMetrics incrementSignins(String type) {
        signins.labels(type).inc();
        return this;
    }
    public PrometheusMetrics incrementSignouts(String type) {
        signouts.labels(type).inc();
        return this;
    }

    /*
    public TimerStopper getRequestTimer(String type) {
        Histogram.Timer requestTimer = requestLatency
                .labels(type)
                .startTimer();
        return () -> {
            requestTimer
                    .observeDuration();
        };
    }
    */

    public PrometheusMetrics observeRequestTime(long latencyInMillis, String type, int statusCode) {
        requestLatency.labels(type, "" + statusCode).observe(latencyInMillis);
        return this;
    }

    public String metrics() throws IOException {
        final StringWriter writer = new StringWriter();
        TextFormat.write004(writer, registry.metricFamilySamples());
        return writer.toString();
    }
}
