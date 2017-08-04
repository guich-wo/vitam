/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 *  In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.serverv2.application;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.metrics.VitamInstrumentedResourceMethodApplicationListener;
import fr.gouv.vitam.common.metrics.VitamMetrics;
import fr.gouv.vitam.common.metrics.VitamMetricsType;
import fr.gouv.vitam.common.server.HeaderIdContainerFilter;
import fr.gouv.vitam.common.server.application.GenericExceptionMapper;
import fr.gouv.vitam.common.server.application.configuration.VitamMetricsConfiguration;

/**
 * list of all business application
 */
public class CommonBusinessApplication {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CommonBusinessApplication.class);

    private static final String METRICS_CONF_FILE_NAME = "vitam.metrics.conf";

    private static final Map<VitamMetricsType, VitamMetrics> metrics = new ConcurrentHashMap<>();

    private Set<Object> resources;

    public CommonBusinessApplication() {
        this.resources = new HashSet<>();
        resources.add(new HeaderIdContainerFilter());
        resources.add(new GenericExceptionMapper());
        clearAndconfigureMetrics();
        if (metrics.containsKey(VitamMetricsType.JERSEY)) {
            resources.add(new VitamInstrumentedResourceMethodApplicationListener(
                metrics.get(VitamMetricsType.JERSEY).getRegistry()));
        }
        startMetrics();
    }

    public Set<Object> getResources() {
        return resources;
    }

    /**
     * Clear the metrics map from any existing {@code VitamMetrics} and reload the configuration from the
     * {@code #METRICS_CONF_FILE_NAME}
     */
    protected static final void clearAndconfigureMetrics() {
        VitamMetricsConfiguration metricsConfiguration = new VitamMetricsConfiguration();

        metrics.clear();
        // Throws a JsonMappingException when the vitam.metrics.conf file is empty
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(METRICS_CONF_FILE_NAME)) {
            metricsConfiguration = PropertiesUtils.readYaml(yamlIS, VitamMetricsConfiguration.class);
        } catch (final IOException e) {
            LOGGER.warn(e.getMessage());
        }

        if (metricsConfiguration.hasMetricsJersey()) {
            metrics.put(VitamMetricsType.JERSEY, new VitamMetrics(VitamMetricsType.JERSEY, metricsConfiguration));
        }
        if (metricsConfiguration.hasMetricsJVM()) {
            metrics.put(VitamMetricsType.JVM, new VitamMetrics(VitamMetricsType.JVM, metricsConfiguration));
        }
        metrics.put(VitamMetricsType.BUSINESS, new VitamMetrics(VitamMetricsType.BUSINESS, metricsConfiguration));
    }

    /**
     * Start the reporting of every metrics
     */
    public final void startMetrics() {
        for (final Map.Entry<VitamMetricsType, VitamMetrics> entry : metrics.entrySet()) {
            entry.getValue().start();
        }
    }

}