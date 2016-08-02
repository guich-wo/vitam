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
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ihmdemo.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.builder.request.construct.Select;
import fr.gouv.vitam.builder.request.construct.Update;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.parser.request.parser.RequestParser;
import fr.gouv.vitam.parser.request.parser.RequestParserHelper;
import fr.gouv.vitam.parser.request.parser.SelectParser;
import fr.gouv.vitam.parser.request.parser.UpdateParser;

/**
 * DslQueryHelper junit test
 *
 */
public class DslQueryHelperTest {

    private static final String result =
        "QUERY: Requests: \n" + "{\"$and\":[" + "{\"$eq\":{\"date\":\"2006-03-05\"}}," +
            "{\"$eq\":{\"events.obIdIn\":\"name\"}}," + "{\"$eq\":{\"evTypeProc\":\"INGEST\"}}," +
            "{\"$eq\":{\"title\":\"Archive2\"}}]}\n" + "\tFilter: {\"$limit\":10000,\"$orderby\":{\"evDateTime\":-1}}\n" +
            "\tRoots: []\n" + "\tProjection: {}\n" + "\tLastLevel: 1";

    private static final String updateAction =
        "[ " + "{ $set : { mavar1 : 1, mavar2 : 1.2, mavar3 : true, mavar4 : 'ma chaine' } }," +
            "{ $unset : [ 'mavar5', 'mavar6' ] }," + "{ $inc : { mavar7 : 2 } }," + "{ $min : { mavar8 : 3 } }," +
            "{ $min : { mavar16 : 12 } }," + "{ $max : { mavar9 : 3 } }," + "{ $rename : { mavar10 : 'mavar11' } }," +
            "{ $push : { mavar12 : { $each : [ 1, 2 ] } } }," + "{ $add : { mavar13 : { $each : [ 1, 2 ] } } }," +
            "{ $pop : { mavar14 : -1 } }," + "{ $pull : { mavar15 : { $each : [ 1, 2 ] } } } ]";

    private static final String exampleUpdateMd = "{ $roots : [ 'id0' ], $query : [ " + "{ $path : [ 'id1', 'id2'] }," +
        "{ $and : [ {$exists : 'mavar1'}, {$missing : 'mavar2'}, {$isNull : 'mavar3'}, { $or : [ {$in : { 'mavar4' : [1, 2, 'maval1'] }}, { $nin : { 'mavar5' : ['maval2', true] } } ] } ] }," +
        "{ $not : [ { $size : { 'mavar5' : 5 } }, { $gt : { 'mavar6' : 7 } }, { $lte : { 'mavar7' : 8 } } ] , $exactdepth : 4}," +
        "{ $not : [ { $eq : { 'mavar8' : 5 } }, { $ne : { 'mavar9' : 'ab' } }, { $range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}, " +
        "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
        "{ $regex : { 'mavar14' : '^start?aa.*' }, $depth : -1 } " + "], " + "$filter : {$mult : false }," +
        "$action : " + updateAction + " }";

    /**
             * Tests createLogBookSelectDSLQuery method : main scenario
             * 
             * @throws InvalidCreateOperationException
             * @throws InvalidParseOperationException
             */
            @Test
            public void testCreateSingleQueryDSL()
                throws InvalidCreateOperationException, InvalidParseOperationException {
        
                HashMap<String, String> myHashMap = new HashMap<String, String>();
                myHashMap.put("title", "Archive2");
                myHashMap.put("date", "2006-03-05");
                myHashMap.put("orderby", "evDateTime");
                myHashMap.put("obIdIn", "name");
                myHashMap.put("INGEST", "date");
        
                String request = DslQueryHelper.createSingleQueryDSL(myHashMap);
                assertNotNull(request);
                final SelectParser request2 = new SelectParser();
                request2.parse(request);
                assertEquals(result, request2.toString());
            }

    /**
     * Tests CreateSelectDSLQuery mthod : main scenario
     * 
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test
    public void testCreateSelectDSLQuery() throws InvalidParseOperationException, InvalidCreateOperationException {
        Map<String, String> queryMap = new HashMap<String, String>();
        queryMap.put("title", "Archive2");
        queryMap.put("orderby", "date");
        queryMap.put("projection_", "#id");
        queryMap.put(UiConstants.SELECT_BY_ID.toString(), "1");

        String selectRequest = DslQueryHelper.createSelectDSLQuery(queryMap);
        assertNotNull(selectRequest);

        JsonNode selectRequestJsonNode = JsonHandler.getFromString(selectRequest);

        RequestParser selectParser = RequestParserHelper.getParser(selectRequestJsonNode);
        assertTrue(selectParser instanceof SelectParser);
        assertTrue(((Select) selectParser.getRequest()).getNbQueries() == 1);
        assertTrue(((Select) selectParser.getRequest()).getRoots().size() == 1);
        assertTrue(((Select) selectParser.getRequest()).getFilter().get("$orderby") != null);
        assertTrue(((Select) selectParser.getRequest()).getProjection().size() == 1);
    }

    /**
     * Tests CreateSelectDSLQuery with empty queries part
     * 
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test
    public void testEmptyQueries() throws InvalidParseOperationException, InvalidCreateOperationException {
        Map<String, String> queryMap = new HashMap<String, String>();
        queryMap.put("projection_", "#id");

        String selectRequest = DslQueryHelper.createSelectDSLQuery(queryMap);
        assertNotNull(selectRequest);

        JsonNode selectRequestJsonNode = JsonHandler.getFromString(selectRequest);

        RequestParser selectParser = RequestParserHelper.getParser(selectRequestJsonNode);
        assertTrue(selectParser instanceof SelectParser);
        assertTrue(((Select) selectParser.getRequest()).getNbQueries() == 0);
        assertTrue(((Select) selectParser.getRequest()).getRoots().size() == 0);
        assertTrue(((Select) selectParser.getRequest()).getFilter().get("$orderby") == null);
        assertTrue(((Select) selectParser.getRequest()).getProjection().size() == 1);
    }

    /**
     * Tests CreateSelectDSLQuery with invalid input parameter (empty value)
     * 
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test(expected = InvalidParseOperationException.class)
    public void testInvalidParseOperationExceptionWithEmptyValue()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        Map<String, String> queryMap = new HashMap<String, String>();
        queryMap.put("title", "");
        DslQueryHelper.createSelectDSLQuery(queryMap);
    }

    /**
     * Tests CreateSelectDSLQuery with invalid input parameter (empty key)
     * 
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test(expected = InvalidParseOperationException.class)
    public void testInvalidParseOperationExceptionWithEmptyKey()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        Map<String, String> queryMap = new HashMap<String, String>();
        queryMap.put("", "value");
        DslQueryHelper.createSelectDSLQuery(queryMap);
    }

    /**
     * Tests CreateUpdateDSLQuery mthod : main scenario
     * 
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test
    public void testCreateUpdateDSLQuery() throws InvalidParseOperationException, InvalidCreateOperationException {
        Map<String, String> queryMap = new HashMap<String, String>();
        queryMap.put("title", "Archive2");
        queryMap.put("date", "09/09/2015");
        queryMap.put(UiConstants.SELECT_BY_ID.toString(), "#id");
        String updateRequest = DslQueryHelper.createUpdateDSLQuery(queryMap);
        assertNotNull(updateRequest);

        JsonNode updateRequestJsonNode = JsonHandler.getFromString(updateRequest);

        RequestParser updateParser = RequestParserHelper.getParser(updateRequestJsonNode);
        assertTrue(updateParser instanceof UpdateParser);
        assertTrue(((Update) updateParser.getRequest()).getActions().size() == 2);
        assertTrue(((Update) updateParser.getRequest()).getRoots().size() == 1);

    }

    /**
     * Tests CreateUpdateDSLQuery with empty queries part
     * 
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test
    public void testUpdateEmptyQueries() throws InvalidParseOperationException, InvalidCreateOperationException {
        Map<String, String> queryMap = new HashMap<String, String>();
        queryMap.put("title", "Mosqueteers");

        String updateRequest = DslQueryHelper.createUpdateDSLQuery(queryMap);
        assertNotNull(updateRequest);

        JsonNode updateRequestJsonNode = JsonHandler.getFromString(updateRequest);

        RequestParser updateParser = RequestParserHelper.getParser(updateRequestJsonNode);
        assertTrue(updateParser instanceof UpdateParser);
        assertTrue(((Update) updateParser.getRequest()).getActions().size() == 1);
    }

    /**
     * Tests CreateUpdateDSLQuery with invalid input parameter (empty value)
     * 
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test(expected = InvalidParseOperationException.class)
    public void testUpdateQueryInvalidParseOperationExceptionWithEmptyValue()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        Map<String, String> queryMap = new HashMap<String, String>();
        queryMap.put("title", "");
        DslQueryHelper.createUpdateDSLQuery(queryMap);
    }

    /**
     * Tests CreateUpdateDSLQuery with invalid input parameter (empty key)
     * 
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    @Test(expected = InvalidParseOperationException.class)
    public void testUpdateQueryInvalidParseOperationExceptionWithEmptyKey()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        Map<String, String> queryMap = new HashMap<String, String>();
        queryMap.put("", "value");
        DslQueryHelper.createUpdateDSLQuery(queryMap);
    }

}
