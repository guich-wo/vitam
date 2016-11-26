/*******************************************************************************
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
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.ingest.internal.client;

import java.io.IOException;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * IngestInternal client factory<br>
 *
 * Used to create IngestInternal client : if configuration file does not exist {@value 'ingest-internal-client.conf'}},
 * <br>
 * mock IngestInternal client will be returned
 *
 */

public class IngestInternalClientFactory extends VitamClientFactory<IngestInternalClient> {
    private static final String CONFIGURATION_FILENAME = "ingest-internal-client.conf";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalClientFactory.class);
    private static final IngestInternalClientFactory INGEST_INTERNAL_CLIENT_FACTORY = new IngestInternalClientFactory();

    private static final String RESOURCE_PATH = "/ingest/v1";

    private IngestInternalClientFactory() {
        super(changeConfigurationFile(CONFIGURATION_FILENAME), RESOURCE_PATH);
    }

    /**
     * Change client configuration from a Yaml files
     *
     * @param configurationPath the path to the configuration file
     */
    static final IngestInternalClientConfiguration changeConfigurationFile(String configurationPath) {
        IngestInternalClientConfiguration configuration = null;
        try {
            configuration = PropertiesUtils.readYaml(PropertiesUtils.findFile(configurationPath),
                IngestInternalClientConfiguration.class);
        } catch (final IOException fnf) {
            LOGGER
                .debug("Error when retrieving configuration file {}, using mock",
                    CONFIGURATION_FILENAME,
                    fnf);
        }
        if (configuration == null) {
            LOGGER.error("Error when retrieving configuration file {}, using mock",
                CONFIGURATION_FILENAME);
        }
        return configuration;
    }

    /**
     * Get the IngestInternalClientFactory instance
     *
     * @return the instance
     */
    public static final IngestInternalClientFactory getInstance() {
        return INGEST_INTERNAL_CLIENT_FACTORY;
    }

    @Override
    public IngestInternalClient getClient() {
        IngestInternalClient client = null;
        switch (getVitamClientType()) {
            case MOCK:
                client = new IngestInternalClientMock();
                break;
            case PRODUCTION:
                client = new IngestInternalClientRest(this);
                break;
            default:
                throw new IllegalArgumentException("Ingest Internal client type unknown");
        }
        return client;
    }

    /**
     *
     * @param configuration null for MOCK
     */
    static final void changeMode(IngestInternalClientConfiguration configuration) {
        getInstance().initialisation(configuration, getInstance().getResourcePath());
    }
}
