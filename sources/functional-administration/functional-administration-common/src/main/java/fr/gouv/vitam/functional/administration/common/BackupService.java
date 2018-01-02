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
package fr.gouv.vitam.functional.administration.common;


import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.functional.administration.common.exception.BackupServiceException;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.io.InputStream;


/**
 * BackupService class for storing files in offers
 * Thread Safe
 */
public class BackupService {

    private static final String STRATEGY_ID = "default";
    private final WorkspaceClientFactory workspaceClientFactory;
    private final StorageClientFactory storageClientFactory;

    public BackupService() {
        workspaceClientFactory = WorkspaceClientFactory.getInstance();
        storageClientFactory = StorageClientFactory.getInstance();
    }

    @VisibleForTesting BackupService(
        WorkspaceClientFactory workspaceClientFactory,
        StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
    }

    /**
     * Store file in offers
     */
    public void backup(InputStream stream, StorageCollectionType storageCollectionType, String uri)
        throws BackupServiceException {
        WorkspaceClient workspaceClient = workspaceClientFactory.getClient();
        StorageClient storageClient = storageClientFactory.getClient();
        try {

            //store in workSpace
            workspaceClient.createContainer(uri);
            workspaceClient.putObject(uri, uri, stream);

            //store in offer
            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(uri);
            description.setWorkspaceObjectURI(uri);
            storageClient.storeFileFromWorkspace(STRATEGY_ID, storageCollectionType, uri, description);

            //deleteContainer
            workspaceClient.deleteContainer(uri, true);


        } catch (ContentAddressableStorageServerException
            | ContentAddressableStorageAlreadyExistException e) {
            //workspace Error
            throw new BackupServiceException("Unable to store file in workSpace");

        } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException | StorageServerClientException e) {
            //Offer storage Error
            throw new BackupServiceException("Unable to store file from workSpace");
        } catch (ContentAddressableStorageNotFoundException e) {
            // clean container Error
            throw new BackupServiceException("Unable to delete file");
        } finally {
            StreamUtils.closeSilently(stream);
        }
    }

}