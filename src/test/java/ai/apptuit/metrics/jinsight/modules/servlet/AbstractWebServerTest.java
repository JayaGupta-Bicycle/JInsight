/*
 * Copyright 2017 Agilx, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.apptuit.metrics.jinsight.modules.servlet;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import ai.apptuit.metrics.jinsight.RegistryService;
import com.codahale.metrics.Counting;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


/**
 * @author Rajiv Shivane
 */
abstract class AbstractWebServerTest {

  private MetricRegistry registry;
  private Map<String, Counting> counters;


  public void setupMetrics(TagEncodedMetricName requestMetricRoot) throws Exception {
    registry = RegistryService.getMetricRegistry();

    counters = new HashMap<>();
    counters.put("GET", getTimer(requestMetricRoot.withTags("method", "GET")));
    counters.put("POST", getTimer(requestMetricRoot.withTags("method", "POST")));
  }

  protected String getText(HttpURLConnection connection) throws IOException {
    return new Scanner(connection.getInputStream()).useDelimiter("\0").next();
  }

  protected Timer getTimer(TagEncodedMetricName metricName) {
    return registry.timer(metricName.toString());
  }

  protected Map<String, Long> getCurrentCounts() {
    Map<String, Long> currentValues = new HashMap<>(counters.size());
    counters.forEach((k, meter) -> {
      currentValues.put(k, meter.getCount());
    });
    return currentValues;
  }
}