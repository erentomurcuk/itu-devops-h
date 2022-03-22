package Metrics;

import spark.Request;
import spark.Response;

@FunctionalInterface
public interface TimerStopper {
    void handle() throws Exception;
}
