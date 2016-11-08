package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathCosFunc extends XPathFuncExpr {
    private static final String NAME = "cos";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathCosFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathCosFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        evaluateArguments(model, evalContext);

        return cosin(evaluatedArgs[0]);
    }

    /**
     * Returns the cosine of the argument, expressed in radians.
     */
    private static Double cosin(Object o) {
        double value = toDouble(o);
        return Math.cos(value);
    }

}
