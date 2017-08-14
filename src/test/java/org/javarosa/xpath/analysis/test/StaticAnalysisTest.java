package org.javarosa.xpath.analysis.test;

import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.analysis.InstanceNameAccumulatingAnalyzer;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by amstone326 on 8/14/17.
 */

public class StaticAnalysisTest {

    private static String NO_INSTANCES_EXPR =
            "double(now()) > (double(/data/last_viewed) + 10)";
    private static String ONE_INSTANCE_EXPR =
            "instance('casedb')/casedb/case[@case_type='case'][@status='open']";
    private static String DUPLICATED_INSTANCE_EXPR =
            "count(instance('commcaresession')/session/user/data/role) > 0 and " +
                    "instance('commcaresession')/session/user/data/role= 'case_manager'";
    private static String EXPR_WITH_INSTANCE_IN_PREDICATE =
            "instance('casedb')/casedb/case[@case_type='commcare-user']" +
                    "[hq_user_id=instance('commcaresession')/session/context/userid]/@case_id";
    private static String RIDICULOUS_RELEVANCY_CONDITION_FROM_REAL_APP =
            "(instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/current_schedule_phase = 2 " +
                    "and instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/add != '' and " +
                    "(today() >= (date(instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/add) " +
                    "+ int(instance('schedule:m5:p2:f2')/schedule/@starts)) and (instance('schedule:m5:p2:f2')/schedule/@expires = '' " +
                    "or today() >= (date(instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/add) + " +
                    "int(instance('schedule:m5:p2:f2')/schedule/@expires))))) and " +
                    "(instance('schedule:m5:p2:f2')/schedule/@allow_unscheduled = 'True' or " +
                    "count(instance('schedule:m5:p2:f2')/schedule/visit[instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/last_visit_number_cf = '' " +
                    "or if(@repeats = 'True', @id >= instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/last_visit_number_cf, " +
                    "@id > instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/last_visit_number_cf)]" +
                    "[if(@repeats = 'True', today() >= (date(instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/last_visit_date_cf) + " +
                    "int(@increment) + int(@starts)) and (@expires = '' or today() <= (date(instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/last_visit_date_cf) + " +
                    "int(@increment) + int(@expires))), today() >= (date(instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/add) + " +
                    "int(@due) + int(@starts)) and (@expires = '' or today() <= (date(instance('casedb')/casedb/case[@case_id=instance('commcaresession')/session/data/case_id_load_ccs_record0]/add) + " +
                    "int(@due) + int(@expires))))]) > 0)";

    @Test
    public void testInstanceAccumulatingAnalyzer() throws XPathSyntaxException {
        testInstanceAnalysisAsList(NO_INSTANCES_EXPR, new String[]{});
        testInstanceAnalysisAsList(ONE_INSTANCE_EXPR, new String[]{"casedb"});
        testInstanceAnalysisAsSet(DUPLICATED_INSTANCE_EXPR, new String[]{"commcaresession"});
        testInstanceAnalysisAsSet(EXPR_WITH_INSTANCE_IN_PREDICATE, new String[]{"casedb", "commcaresession"});
        testInstanceAnalysisAsSet(RIDICULOUS_RELEVANCY_CONDITION_FROM_REAL_APP, new String[]{"casedb", "commcaresession", "schedule:m5:p2:f2"});

        // Test the length of the result with list accumulation, just to ensure it gets them all
        List<String> parsedInstancesList =
                (new InstanceNameAccumulatingAnalyzer()).accumulateAsList(
                        XPathParseTool.parseXPath(RIDICULOUS_RELEVANCY_CONDITION_FROM_REAL_APP));
        Assert.assertEquals(27, parsedInstancesList.size());
    }

    private void testInstanceAnalysisAsSet(String expressionString, String[] expectedInstances)
            throws XPathSyntaxException {
        InstanceNameAccumulatingAnalyzer analyzer = new InstanceNameAccumulatingAnalyzer();

        Set<String> expectedInstancesSet = new HashSet<>();
        for (String s : expectedInstances) {
            expectedInstancesSet.add(s);
        }

        Set<String> parsedInstancesSet =
                analyzer.accumulateAsSet(XPathParseTool.parseXPath(expressionString));
        Assert.assertEquals(expectedInstancesSet, parsedInstancesSet);
    }

    private void testInstanceAnalysisAsList(String expressionString, String[] expectedInstances)
            throws XPathSyntaxException {
        InstanceNameAccumulatingAnalyzer analyzer = new InstanceNameAccumulatingAnalyzer();

        List<String> expectedInstancesList = new ArrayList<>();
        for (String s : expectedInstances) {
            expectedInstancesList.add(s);
        }

        List<String> parsedInstancesList =
                analyzer.accumulateAsList(XPathParseTool.parseXPath(expressionString));
        Assert.assertEquals(expectedInstancesList, parsedInstancesList);
    }
}
