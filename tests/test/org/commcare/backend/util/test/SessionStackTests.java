package org.commcare.backend.util.test;

import org.commcare.suite.model.Action;
import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.test.utilities.MockApp;
import org.commcare.util.mocks.SessionWrapper;
import org.javarosa.core.model.condition.EvaluationContext;
import org.junit.Assert;

import org.commcare.util.SessionFrame;
import org.junit.Test;

/**
 * This is a super basic test just to make sure the test infrastructure is working correctly
 * and to act as an example of how to build template app tests.
 *
 * Created by ctsims on 8/14/2015.
 */
public class SessionStackTests {
    MockApp mApp;

    @Test
    public void testDoubleManagementAndOverlappingStack() throws Exception {
        mApp = new MockApp("/complex_stack/");
        SessionWrapper session = mApp.getSession();

        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        session.setCommand("m0");

        Assert.assertEquals(SessionFrame.STATE_DATUM_COMPUTED, session.getNeededData());

        session.setComputedDatum();

        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());
        Assert.assertEquals("case_id", session.getNeededDatum().getDataId());

        Action dblManagement = session.getDetail(session.getNeededDatum().getShortDetail()).getCustomAction();

        if(dblManagement == null) {
            Assert.fail("Detail screen stack action was missing from app!");
        }

        session.executeStackOperations(dblManagement.getStackOperations(), session.getEvaluationContext());

        if(session.getNeededData() != null) {
            Assert.fail("After executing stack frame steps, session should be redirected");
        }

        Assert.assertEquals("http://commcarehq.org/test/placeholder_destination", session.getForm());

        EvaluationContext ec = session.getEvaluationContext();

        CaseTestUtils.xpathEvalAndCompare(ec,"count(instance('session')/session/data/calculated_data)", 1);

        CaseTestUtils.xpathEvalAndCompare(ec,"instance('session')/session/data/calculated_data", "new");
    }

    @Test
    public void testViewNav() throws Exception {
        mApp = new MockApp("/complex_stack/");
        SessionWrapper session = mApp.getSession();

        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        session.setCommand("m3-f0");

        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());

        Assert.assertEquals("case_id_to_send", session.getNeededDatum().getDataId());

        Assert.assertFalse("Session incorrectly determined a view command", session.isViewCommand(session.getCommand()));

        session.setDatum("case_id_to_send", "case_one");

        session.finishExecuteAndPop(session.getEvaluationContext());

        Assert.assertEquals("m2", session.getCommand());

        CaseTestUtils.xpathEvalAndCompare(session.getEvaluationContext(),
                "instance('session')/session/data/case_id", "case_one");

        CaseTestUtils.xpathEvalAndCompare(session.getEvaluationContext(),
                "count(instance('session')/session/data/case_id_to_send)", "0");

        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
    }

    @Test
    public void testViewNonNav() throws Exception {
        mApp = new MockApp("/complex_stack/");
        SessionWrapper session = mApp.getSession();

        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());

        session.setCommand("m4-f0");

        Assert.assertEquals(SessionFrame.STATE_DATUM_VAL, session.getNeededData());

        Assert.assertEquals("case_id_to_view", session.getNeededDatum().getDataId());

        Assert.assertTrue("Session incorrectly tagged a view command", session.isViewCommand(session.getCommand()));
    }

    @Test
    public void testOutOfOrderStack() throws Exception {
        mApp = new MockApp("/session-tests-template/");
        SessionWrapper session = mApp.getSession();

        // Select a form that has 3 datum requirements to enter (in order from suite.xml: case_id,
        // case_id_new_visit_0, usercase_id)
        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m0");

        Assert.assertEquals(SessionFrame.STATE_COMMAND_ID, session.getNeededData());
        session.setCommand("m0-f3");

        // Set 2 of the 3 needed datums, but not in order (1st and 3rd)
        session.setDatum("case_id", "case_id_value");
        session.setDatum("usercase_id", "usercase_id_value");

        // Session should now need the case_id_new_visit_0, which is a computed datum
        Assert.assertEquals(SessionFrame.STATE_DATUM_COMPUTED, session.getNeededData());

        // The key of the needed datum should be "case_id_new_visit_0"
        Assert.assertEquals("case_id_new_visit_0", session.getNeededDatum().getDataId());

        // Add the needed datum to the stack and confirm that the session is now ready to proceed
        session.setDatum("case_id_new_visit_0", "visit_id_value");
        Assert.assertEquals(null, session.getNeededData());
    }

}
