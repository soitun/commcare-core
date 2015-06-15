package org.javarosa.xpath.expr.test;

import org.junit.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.test_utils.FormLoadingUtils;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.xpath.IExprDataType;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.XPathUnhandledException;
import org.javarosa.xpath.XPathUnsupportedException;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.expr.XPathNumericLiteral;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author Phillip Mates
 */

public class XPathPathExprTest extends TestCase {
    public XPathPathExprTest(String name) {
        super(name);
    }

    public XPathPathExprTest() {
        super();
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        suite.addTest(new XPathPathExprTest("testHeterogeneousPaths"));
        suite.addTest(new XPathPathExprTest("testNestedMultiplicities"));

        return suite;
    }

    @Test
    public void testHeterogeneousPaths() {
        FormInstance instance = loadInstance("/test_xpathpathexpr.xml");

        // Used to reproduce bug where locations can't handle heterogeneous template paths.
        // This bug has been fixed and the following test now passes.
        testEval("/data/places/country[@id ='two']/state[@id = 'beehive_state']", instance, null, "Utah");
        testEval("/data/places/country[@id ='one']/name", instance, null, "Singapore");
    }

    /**
     * Some simple xpath expressions with multiple predicates that operate over
     * nodesets.
     */
    @Test
    public void testNestedMultiplicities() {
        FormParseInit fpi = new FormParseInit("/test_nested_multiplicities.xml");
        FormDef fd = fpi.getFormDef();
        FormEntryModel fem = fpi.getFormEntryModel();

        testEval("/data/bikes/manufacturer/model[@id='pista']/@color",
                fd.getInstance(), null, "seafoam");
        testEval("join(' ', /data/bikes/manufacturer[@american='yes']/model[.=1]/@id)",
                fd.getInstance(), null, "karate-monkey vamoots");
        testEval("count(/data/bikes/manufacturer[@american='yes'][count(model[.=1]) > 0]/model/@id)",
                fd.getInstance(), null, 4.0);
        testEval("join(' ', /data/bikes/manufacturer[@american='yes'][count(model[.=1]) > 0]/model/@id)",
                fd.getInstance(), null, "karate-monkey long-haul cross-check vamoots");
        testEval("join(' ', /data/bikes/manufacturer[@american='yes'][count(model=1) > 0]/model/@id)",
                fd.getInstance(), null, new XPathTypeMismatchException());
        testEval("join(' ', /data/bikes/manufacturer[@american='no'][model=1]/model/@id)",
                fd.getInstance(), null, new XPathTypeMismatchException());
    }

    private void testEval(String expr, FormInstance model, EvaluationContext ec, Object expected) {
        testEval(expr, model, ec, expected, 1.0e-12);
    }

    private void testEval(String expr, FormInstance model, EvaluationContext ec, Object expected, double tolerance) {
        XPathExpression xpe = null;
        boolean exceptionExpected = (expected instanceof XPathException);

        if (ec == null) {
            ec = new EvaluationContext(model);
        }

        try {
            xpe = XPathParseTool.parseXPath(expr);
        } catch (XPathSyntaxException xpse) {
        }

        if (xpe == null) {
            fail("Null expression or syntax error " + expr);
        }

        try {
            Object result = XPathFuncExpr.unpack(xpe.eval(model, ec));
            if (tolerance != XPathFuncExpr.DOUBLE_TOLERANCE) {
                System.out.println(expr + " = " + result);
            }

            if (exceptionExpected) {
                fail("Expected exception, expression : " + expr);
            } else if ((result instanceof Double && expected instanceof Double)) {
                Double o = ((Double)result).doubleValue();
                Double t = ((Double)expected).doubleValue();
                if (Math.abs(o - t) > tolerance) {
                    fail("Doubles outside of tolerance: got " + o + ", expected " + t);
                }
            } else if (!expected.equals(result)) {
                fail("Expected " + expected + ", got " + result);
            }
        } catch (XPathException xpex) {
            if (!exceptionExpected) {
                fail("Did not expect " + xpex.getClass() + " exception");
            } else if (xpex.getClass() != expected.getClass()) {
                fail("Did not get expected exception type");
            }
        }
    }

    /**
     * Load a form instance from a path.
     * Doesn't create a model or main instance.
     *
     * @param formPath path of the form to load, relative to project build
     * @return FormInstance created from the path pointed to, or null if any
     * error occurs.
     */
    private FormInstance loadInstance(String formPath) {
        FormInstance instance = null;
        try {
            instance = FormLoadingUtils.loadFormInstance(formPath);
        } catch (IOException e) {
            fail("Unable to load form at " + formPath);
        } catch (InvalidStructureException e) {
            fail("Form at " + formPath + " has an invalid structure.");
        }
        return instance;
    }
}
