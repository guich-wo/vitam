/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.logbook.common.server.database.collections.request;

import static fr.gouv.vitam.builder.request.construct.QueryHelper.and;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.eq;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.exists;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.flt;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.gt;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.gte;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.in;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.isNull;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.lt;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.lte;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.match;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.matchPhrase;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.matchPhrasePrefix;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.missing;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.mlt;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.ne;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.nin;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.not;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.or;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.path;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.prefix;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.range;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.regex;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.search;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.size;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.term;
import static fr.gouv.vitam.builder.request.construct.QueryHelper.wildcard;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.builder.request.construct.configuration.GlobalDatas;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.PROJECTION;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.SELECTFILTER;
import fr.gouv.vitam.builder.request.construct.query.Query;
import fr.gouv.vitam.builder.singlerequest.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.MongoDbAccessImpl;
import fr.gouv.vitam.parser.request.parser.GlobalDatasParser;

@SuppressWarnings("javadoc")
public class SelectParserTest {
    private static final String EX_BOTH_ES_MD = "{ $query : " + "{ $and : [ " + "{$exists : 'mavar1'}, " +
        "{$missing : 'mavar2'}, " + "{$isNull : 'mavar3'}, " + "{ $or : [ " +
        "{$in : { 'mavar4' : [1, 2, 'maval1'] } }, " + "{ $nin : { 'mavar5' : ['maval2', true] } } ] }," +
        "{ $not : [ " + "{ $size : { 'mavar5' : 5 } }, " + "{ $gt : { 'mavar6' : 7 } }, " +
        "{ $lte : { 'mavar7' : 8 } },  { $gte : { 'mavar7' : 8 } }, { $lt : { 'mavar7' : 8 } } ] }," + "{ $not : [ " +
        "{ $eq : { 'mavar8' : 5 } }, { " +
        "$ne : { 'mavar9' : 'ab' } }, { " + "$range : { 'mavar10' : { $gte : 12, $lte : 20} } } ] }," +
        "{ $match_phrase : { 'mavar11' : 'ceci est une phrase' } }," +
        "{ $match_phrase_prefix : { 'mavar11' : 'ceci est une phrase', $max_expansions : 10 } }," +
        "{ $flt : { $fields : [ 'mavar12', 'mavar13' ], $like : 'ceci est une phrase' } }," +
        "{ $mlt : { $fields : [ 'mavar12', 'mavar13' ], $like : 'ceci est une phrase' } }," +
        "{ $and : [ " +
        "{ $search : { 'mavar13' : 'ceci est une phrase' } }, " +
        "{ $prefix : { 'mavar13' : 'ceci est une phrase' } }, " +
        "{ $wildcard : { 'mavar13' : 'ceci' } }, " + "{ $regex : { 'mavar14' : '^start?aa.*' } } ] }," +
        "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " + "{ $and : [ " +
        "{ $term : { 'mavar16' : 'motMajuscule', 'mavar17' : 'simplemot' } }, " +
        "{ $or : [ {$eq : { 'mavar19' : 'abcd' } }, { $match : { 'mavar18' : 'quelques mots' } } ] } ] }, " +
        "{ $regex : { 'mavar14' : '^start?aa.*' } } " + "] } , " +
        "$filter : {$offset : 100, $limit : 1000, $hint : ['cache'], " +
        "$orderby : { maclef1 : 1 , maclef2 : -1,  maclef3 : 1 } }," +
        "$projection : {$fields : {#dua : 1, #all : 1} } }";

    private static final String EX_MD = "{ $query : " + "{ $and : [ " + "{$exists : 'mavar1'}, " +
        "{$missing : 'mavar2'}, " + "{$isNull : 'mavar3'}, " + "{ $or : [ " +
        "{$in : { 'mavar4' : [1, 2, 'maval1'] } }, " + "{ $nin : { 'mavar5' : ['maval2', true] } } ] }," +
        "{ $not : [ " + "{ $size : { 'mavar5' : 5 } }, " + "{ $gt : { 'mavar6' : 7 } }, " +
        "{ $lte : { 'mavar7' : 8 } } ] }," + "{ $not : [ " + "{ $eq : { 'mavar8' : 5 } }, { " +
        "$ne : { 'mavar9' : 'ab' } }, { " + "$range : { 'mavar10' : { $gte : 12, $lte : 20} } } ] }," +
        "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
        "{ $regex : { 'mavar14' : '^start?aa.*' } } " + "] } , " +
        "$filter : {$offset : 100, $limit : 1000, $hint : ['cache'], " +
        "$orderby : { maclef1 : 1 , maclef2 : -1,  maclef3 : 1 } }," +
        "$projection : {$fields : {#dua : 1, #all : 1} } }";

    private static final String EX_MD2 = "{ $query : " + "{ $and : [ " + "{$exists : 'mavar1'}, " +
        "{$missing : 'mavar2'}, " + "{$isNull : 'mavar3'}, " + "{ $or : [ " +
        "{$in : { 'mavar4' : [1, 2, 'maval1'] } }, " + "{ $nin : { 'mavar5' : ['maval2', true] } } ] }," +
        "{ $not : [ " + "{ $size : { 'mavar5' : 5 } }, " + "{ $gt : { 'mavar6' : 7 } }, " +
        "{ $lte : { 'mavar7' : 8 } } ] }," + "{ $not : [ " + "{ $eq : { 'mavar8' : 5 } }, { " +
        "$ne : { 'mavar9' : 'ab' } }, { " + "$range : { 'mavar10' : { $gte : 12, $lte : 20} } } ] }," +
        "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
        "{ $regex : { 'mavar14' : '^start?aa.*' } } " + "] } , " +
        "$filter : { " +
        "$orderby : { maclef1 : 1 , maclef2 : -1,  maclef3 : 1 } }," +
        "$projection : {$fields : {#dua : 1, myvar : 1} } }";
    static final ObjectNode DEFAULT_SLICE = JsonHandler.createObjectNode();
    static final ObjectNode DEFAULT_ALLKEYS = JsonHandler.createObjectNode();

    static {
        DEFAULT_SLICE.putObject(LogbookDocument.EVENTS).put(MongoDbAccessImpl.SLICE, -1);
        for (final LogbookMongoDbName name : LogbookMongoDbName.values()) {
            DEFAULT_ALLKEYS.put(name.getDbname(), 1);
        }
    }

    @Before
    public void init() {
        VitamLoggerFactory.setLogLevel(VitamLogLevel.INFO);
    }

    private static String createLongString(int size) {
        final StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    @Test
    public void testSanityCheckRequest() {
        final int previous = GlobalDatasParser.limitRequest;
        GlobalDatasParser.limitRequest = 100;
        try {
            final String longfalsecode = createLongString(GlobalDatasParser.limitRequest + 100);
            final SelectParser request1 = new SelectParser();
            request1.parse(longfalsecode);
            fail("Should fail");
        } catch (final InvalidParseOperationException e) {
            // ignore
        } finally {
            GlobalDatasParser.limitRequest = previous;
        }
    }

    @Test
    public void testParse() {
        try {
            final SelectParser request1 = new SelectParser();
            request1.parse(EX_BOTH_ES_MD);
            assertTrue("Should refuse the request since ES is not allowed",
                request1.hasFullTextQuery());
            request1.parse(EX_MD);
            assertFalse("Should accept the request since ES is not allowed",
                request1.hasFullTextQuery());
        } catch (final Exception e) {}
        try {
            final SelectParser request1 = new SelectParser();
            request1.parse(EX_BOTH_ES_MD);
            assertNotNull(request1);
            assertTrue("Should refuse the request since ES is not allowed",
                request1.hasFullTextQuery());
            final Select select = new Select();
            select.setQuery(path("id1"));
            select.setQuery(
                and().add(exists("mavar1"), missing("mavar2"), isNull("mavar3"),
                    or().add(in("mavar4", 1, 2).add("maval1"),
                        nin("mavar5", "maval2").add(true)),
                    not().add(size("mavar5", 5), gt("mavar6", 7), lte("mavar7", 8),
                        gte("mavar7", 8), lt("mavar7", 8)),
                    not().add(eq("mavar8", 5), ne("mavar9", "ab"),
                        range("mavar10", 12, true, 20, true)),
                    matchPhrase("mavar11", "ceci est une phrase"),
                    matchPhrasePrefix("mavar11", "ceci est une phrase")
                        .setMatchMaxExpansions(10),
                    flt("ceci est une phrase", "mavar12", "mavar13"),
                    mlt("ceci est une phrase", "mavar12", "mavar13"),
                    and().add(search("mavar13", "ceci est une phrase"),
                        prefix("mavar13", "ceci est une phrase"),
                        wildcard("mavar13", "ceci"),
                        regex("mavar14", "^start?aa.*")),
                    and().add(term("mavar14", "motMajuscule").add("mavar15", "simplemot")),
                    and().add(term("mavar16", "motMajuscule").add("mavar17", "simplemot"),
                        or().add(eq("mavar19", "abcd"),
                            match("mavar18", "quelques mots"))),
                    regex("mavar14", "^start?aa.*")));
            select.setLimitFilter(100, 1000).addHintFilter(FILTERARGS.CACHE.exactToken());
            select.addOrderByAscFilter("maclef1")
                .addOrderByDescFilter("maclef2").addOrderByAscFilter("maclef3");
            select.addUsedProjection("#dua", "#all");
            final SelectParser request2 = new SelectParser();
            request2.parse(select.getFinalSelect().toString());
            assertNotNull(request2);
            final Query query1 = request1.getRequest().getQuery();
            final Query query2 = request2.getRequest().getQuery();
            if (!query1.toString().equals(query2.toString())) {
                System.err.println(query1);
                System.err.println(query2);
            }
            assertTrue("TypeRequest should be equal",
                query1.toString().equals(query2.toString()));
            assertTrue("Projection should be equal",
                request1.getRequest().getProjection().toString()
                    .equals(request2.getRequest().getProjection().toString()));
            assertTrue("OrderBy should be equal",
                request1.getRequest().getFilter().toString()
                    .equals(request2.getRequest().getFilter().toString()));
            assertEquals(request1.hasFullTextQuery(), request2.hasFullTextQuery());
            assertEquals(request1.getRequest().getFinalSelect().toString(),
                request2.getRequest().getFinalSelect().toString());
            assertTrue("Command should be equal",
                request1.toString().equals(request2.toString()));
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testFilterParse() {
        final SelectParser request = new SelectParser();
        final Select select = new Select();
        try {
            // empty
            request.filterParse(select.getFilter());
            assertNull("Hint should be null",
                request.getRequest().getFilter().get(SELECTFILTER.HINT.exactToken()));
            assertNotNull("Limit should not be null", request.getRequest().getFilter()
                .get(SELECTFILTER.LIMIT.exactToken()));
            assertNull("Offset should be null", request.getRequest().getFilter()
                .get(SELECTFILTER.OFFSET.exactToken()));
            assertNull("OrderBy should be null", request.getRequest().getFilter()
                .get(SELECTFILTER.ORDERBY.exactToken()));
            // hint set
            select.addHintFilter(FILTERARGS.CACHE.exactToken());
            request.filterParse(select.getFilter());
            assertEquals("Hint should be True", FILTERARGS.CACHE.exactToken(),
                request.getRequest().getFilter().get(SELECTFILTER.HINT.exactToken())
                    .get(0).asText());
            // hint reset
            select.resetHintFilter();
            request.filterParse(select.getFilter());
            assertNull("Hint should be null",
                request.getRequest().getFilter().get(SELECTFILTER.HINT.exactToken()));
            // limit set
            select.setLimitFilter(0, 1000);
            request.filterParse(select.getFilter());
            assertEquals(1000,
                request.getRequest().getFilter().get(SELECTFILTER.LIMIT.exactToken())
                    .asLong());
            assertNull("Offset should be null", request.getRequest().getFilter()
                .get(SELECTFILTER.OFFSET.exactToken()));
            // offset set
            select.setLimitFilter(100, 0);
            request.filterParse(select.getFilter());
            assertEquals(100,
                request.getRequest().getFilter().get(SELECTFILTER.OFFSET.exactToken())
                    .asLong());
            assertEquals(GlobalDatas.limitLoad,
                request.getRequest().getFilter().get(SELECTFILTER.LIMIT.exactToken())
                    .asLong());
            // orderBy set through array
            select.addOrderByAscFilter("var1", "var2").addOrderByDescFilter("var3");
            request.filterParse(select.getFilter());
            assertNotNull("OrderBy should not be null", request.getRequest().getFilter()
                .get(SELECTFILTER.ORDERBY.exactToken()));
            // check both
            assertEquals(3, request.getRequest().getFilter()
                .get(SELECTFILTER.ORDERBY.exactToken()).size());
            for (final Iterator<Entry<String, JsonNode>> iterator =
                request.getRequest().getFilter()
                    .get(SELECTFILTER.ORDERBY.exactToken()).fields(); iterator
                        .hasNext();) {
                final Entry<String, JsonNode> entry = iterator.next();
                if (entry.getKey().equals("var1")) {
                    assertEquals(1, entry.getValue().asInt());
                }
                if (entry.getKey().equals("var2")) {
                    assertEquals(1, entry.getValue().asInt());
                }
                if (entry.getKey().equals("var3")) {
                    assertEquals(-1, entry.getValue().asInt());
                }
            }
            // orderBy set through composite
            select.resetOrderByFilter();
            request.filterParse(select.getFilter());
            assertNull("OrderBy should be null", request.getRequest().getFilter()
                .get(SELECTFILTER.ORDERBY.exactToken()));
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testProjectionParse() {
        final SelectParser request = new SelectParser();
        final Select select = new Select();
        try {
            // empty rootNode
            request.projectionParse(select.getProjection());
            assertEquals("Projection should be empty", 0,
                request.getRequest().getProjection().size());
            // projection set but empty
            select.addUsedProjection((String) null);
            // empty set
            request.projectionParse(select.getProjection());
            assertEquals("Projection should not be empty", 0,
                request.getRequest().getProjection().size());
            // projection set
            select.addUsedProjection("var");
            // empty set
            request.projectionParse(select.getProjection());
            assertEquals("Projection should not be empty", 1,
                request.getRequest().getProjection().size());
            // reset
            select.resetUsageProjection().resetUsedProjection();
            request.projectionParse(select.getProjection());
            assertEquals("Projection should be empty", 0,
                request.getRequest().getProjection().size());
            // not empty set
            select.addUsedProjection("var1").addUnusedProjection("var2");
            request.projectionParse(select.getProjection());
            assertEquals("Projection should not be empty", 1,
                request.getRequest().getProjection().size());
            assertEquals(2, request.getRequest().getProjection()
                .get(PROJECTION.FIELDS.exactToken()).size());
            for (final Iterator<Entry<String, JsonNode>> iterator =
                request.getRequest().getProjection()
                    .get(PROJECTION.FIELDS.exactToken()).fields(); iterator
                        .hasNext();) {
                final Entry<String, JsonNode> entry = iterator.next();
                if (entry.getKey().equals("var1")) {
                    assertEquals(1, entry.getValue().asInt());
                }
                if (entry.getKey().equals("var2")) {
                    assertEquals(0, entry.getValue().asInt());
                }
            }
            try {
                request.projectionParse(null);
            } catch (final InvalidParseOperationException e) {
                fail("Should not raized an exception");
            }
            try {
                request.addProjection(DEFAULT_SLICE, null);
                fail("Should raized an exception");
            } catch (final InvalidParseOperationException e) {
                // Ignore
            }
            try {
                request.addProjection(null, DEFAULT_ALLKEYS);
                fail("Should raized an exception");
            } catch (final InvalidParseOperationException e) {
                // Ignore
            }
            new SelectToMongoDb(request).getFinalProjection();
            try {
                request.addProjection(DEFAULT_SLICE, DEFAULT_ALLKEYS);
                new SelectToMongoDb(request).getFinalProjection();
            } catch (final InvalidParseOperationException e) {
                fail("Should not raized an exception");
            }
            request.projectionParse(JsonHandler.getFromString("{$fields: { var1: -1 }}"));
            new SelectToMongoDb(request).getFinalProjection();
            try {
                request.addProjection(DEFAULT_SLICE, DEFAULT_ALLKEYS);
                new SelectToMongoDb(request).getFinalProjection();
            } catch (final InvalidParseOperationException e) {
                fail("Should not raized an exception");
            }
            request.projectionParse(JsonHandler.getFromString("{}"));
            new SelectToMongoDb(request).getFinalProjection();
            try {
                request.addProjection(DEFAULT_SLICE, DEFAULT_ALLKEYS);
                new SelectToMongoDb(request).getFinalProjection();
            } catch (final InvalidParseOperationException e) {
                fail("Should not raized an exception");
            }
        } catch (final InvalidParseOperationException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testSelectParser() throws InvalidParseOperationException {
        final SelectParser request = new SelectParser();
        final JsonNode req = JsonHandler.getFromString(EX_MD);
        request.parse(req);
        assertNotNull(request);

        final SelectParser request2 = new SelectParser(new LogbookVarNameAdapter());
        assertNotNull(request2);
    }

    @Test
    public void testParseQueryOnly() throws InvalidParseOperationException {
        final SelectParser request = new SelectParser();
        final String ex = "{}";
        request.parseQueryOnly(ex);
        assertNotNull(request);
    }

    @Test
    public void testInternalParseSelect() throws InvalidParseOperationException {
        final SelectParser request = new SelectParser();
        final String s = "[ [ 'id0' ], { $path : [ 'id1', 'id2'] }, {$mult : false }, {} ]";
        request.parse(s);
        assertNotNull(request);
    }

    @Test
    public void testToMongoDb() throws InvalidParseOperationException {
        final SelectParser request = new SelectParser();
        JsonNode req = JsonHandler.getFromString(EX_MD);
        request.parse(req);
        assertNotNull(request);
        SelectToMongoDb selectToMongoDb = new SelectToMongoDb(request);
        assertFalse(selectToMongoDb.hasFullTextQuery());
        assertFalse(selectToMongoDb.hintNoTimeout());
        assertTrue(selectToMongoDb.getHints().size() > 0);
        assertTrue(selectToMongoDb.getFinalOffset() > 0);
        assertNotNull(selectToMongoDb.getFinalOrderBy());
        assertNull(selectToMongoDb.getFinalProjection());

        req = JsonHandler.getFromString(EX_BOTH_ES_MD);
        request.parse(req);
        selectToMongoDb = new SelectToMongoDb(request);
        assertTrue(selectToMongoDb.hasFullTextQuery());
        assertFalse(selectToMongoDb.hintNoTimeout());
        assertTrue(selectToMongoDb.getHints().size() > 0);
        assertTrue(selectToMongoDb.getFinalOffset() > 0);
        assertNotNull(selectToMongoDb.getFinalOrderBy());
        assertNull(selectToMongoDb.getFinalProjection());

        req = JsonHandler.getFromString(EX_MD2);
        request.parse(req);
        selectToMongoDb = new SelectToMongoDb(request);
        assertFalse(selectToMongoDb.hasFullTextQuery());
        assertFalse(selectToMongoDb.hintNoTimeout());
        assertNull(selectToMongoDb.getHints());
        assertTrue(selectToMongoDb.getFinalOffset() == 0);
        assertNotNull(selectToMongoDb.getFinalOrderBy());
        assertNotNull(selectToMongoDb.getFinalProjection());
    }
}
