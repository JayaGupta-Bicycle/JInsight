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

package ai.apptuit.metrics.jinsight;

import ai.apptuit.metrics.dropwizard.ApptuitReporter.ReportingMode;
import ai.apptuit.metrics.dropwizard.ApptuitReporterFactory;
import ai.apptuit.metrics.jinsight.modules.jvm.JvmMetricSet;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides access to the MetricRegistry that is pre-configured to use {@link
 * ai.apptuit.metrics.dropwizard.ApptuitReporter}. Rest the Agent runtime classes use this registry
 * to create metrics.
 *
 * @author Rajiv Shivane
 */
public class RegistryService {

  private static final Logger LOGGER = Logger.getLogger(RegistryService.class.getName());

  private static final RegistryService singleton = new RegistryService();
  private static final String HOST_TAG_NAME = "host";
  private MetricRegistry registry = null;

  private RegistryService() {
    this(ConfigService.getInstance(), new ApptuitReporterFactory());
  }

  RegistryService(ConfigService configService, ApptuitReporterFactory factory) {
    this.registry = new TracingMetricRegistry();

    ReportingMode mode = null;
    String configMode = configService.getReportingMode();
    if (configMode != null) {
      try {
        mode = ReportingMode.valueOf(configMode.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        LOGGER.severe("Un-supported reporting mode [" + configMode + "]");
        LOGGER.log(Level.FINE, e.toString(), e);
      }
    }

    String apiUrl = configService.getApiUrl();
    if (apiUrl != null) {
      try {
        new URL(apiUrl);
      } catch (MalformedURLException e) {
        LOGGER.severe("Malformed API URL [" + apiUrl + "]. Using default URL instead");
        LOGGER.log(Level.FINE, e.toString(), e);
        apiUrl = null;
      }
    }

    ScheduledReporter reporter = createReporter(factory, getGlobalTags(configService),
        configService.getApiToken(), apiUrl, mode);
    reporter.start(5, TimeUnit.SECONDS);

    registry.registerAll(new JvmMetricSet());
  }

  public static MetricRegistry getMetricRegistry() {
    return getRegistryService().registry;
  }

  public static RegistryService getRegistryService() {
    return singleton;
  }

  private ScheduledReporter createReporter(ApptuitReporterFactory factory,
      Map<String, String> globalTags, String apiToken, String apiUrl, ReportingMode reportingMode) {
    factory.setRateUnit(TimeUnit.SECONDS);
    factory.setDurationUnit(TimeUnit.MILLISECONDS);
    globalTags.forEach(factory::addGlobalTag);
    factory.setApiKey(apiToken);
    factory.setApiUrl(apiUrl);
    factory.setReportingMode(reportingMode);

    return factory.build(registry);
  }

  private Map<String, String> getGlobalTags(ConfigService configService) {
    Map<String, String> globalTags = configService.getGlobalTags();
    String hostname = globalTags.get(HOST_TAG_NAME);
    if (hostname != null && !"".equals(hostname.trim())) {
      return globalTags;
    }

    try {
      hostname = InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException e) {
      hostname = "?";
    }
    Map<String, String> retVal = new HashMap<>(globalTags);
    retVal.put(HOST_TAG_NAME, hostname);
    return retVal;
  }
}
