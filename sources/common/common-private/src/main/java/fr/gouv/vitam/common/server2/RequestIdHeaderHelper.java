/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital
 * archiving back-office system managing high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.common.server2;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.exception.VitamThreadAccessException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Helper class to manage the X_REQUEST_ID and VitamSession links
 */
public class RequestIdHeaderHelper {

    public enum Context {
        REQUEST,
        RESPONSE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RequestIdHeaderHelper.class);

    /**
     * Helper class, so not instanciable.
     */
    private RequestIdHeaderHelper() {
        throw new UnsupportedOperationException("Helper class");
    }

    /**
     * Extracts the X-REQUEST-ID from the headers to save it through the VitamSession
     * @param requestHeaders Complete list of HTTP message headers ; will not be changed.
     * @param ctx Context, or rather http message type (request or response)
     */
    public static void putRequestIdFromHeaderInSession(MultivaluedMap<String, String> requestHeaders, Context ctx) {
        try {

            final String requestId = requestHeaders.getFirst(GlobalDataRest.X_REQUEST_ID);
            final VitamSession vitamSession = VitamThreadUtils.getVitamSession();

            if (vitamSession.getRequestId() != null && !vitamSession.getRequestId().equals(requestId)) {
                LOGGER.warn("Caution : the requestId stored in session was not empty and different from the received requestId before {} handling ! Some cleanup must have failed... Old requestId will be discarded in session.", ctx);
            }

            vitamSession.setRequestId(requestId);

            if (requestId != null) {
                LOGGER.debug("Got requestId {} from {} headers ; setting it in the current VitamSession", requestId, ctx);
            } else {
                LOGGER.debug("No requestId found in {} ; setting it as empty in the current VitamSession", ctx);
            }
        } catch (VitamThreadAccessException e) {
            LOGGER.warn("Got an exception while trying to set the requestId {} in the current session ; exception was : {}", requestHeaders, e.getMessage());
            //the processing should not be interrupted by this exception
        }
    }

    /**
     * Retrieves the request id from the VitamSession and add a X-REQUEST-ID header
     * @param headers List of target HTTP headers ; required header will be added to this list.
     * @param ctx Context, or rather http message type (request or response)
     */
    public static void putRequestIdFromSessionInHeader(MultivaluedMap<String, Object> headers, Context ctx) {
        try {
            final String requestId = VitamThreadUtils.getVitamSession().getRequestId();
            if (requestId != null) {
                headers.add(GlobalDataRest.X_REQUEST_ID, requestId);
                LOGGER.debug("RequestId {} found in session and set in the {} header.", requestId, ctx);
            } else {
                LOGGER.warn("No RequestId found in session (somebody should have set it) ! {} header will not be set in the http {}.", GlobalDataRest.X_REQUEST_ID, ctx);
            }
        } catch (VitamThreadAccessException e) {
            LOGGER.warn("Got an exception while trying to get the requestId from the current session ; exception was : {}", e.getMessage());
            //the processing should not be interrupted by this exception
        }
    }



}
