package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.XPathUnhandledException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Custom function that is dispatched at runtime
 */
public class XPathCustomFunc extends XPathFuncExpr {

    public XPathCustomFunc() {
    }

    public XPathCustomFunc(String name, XPathExpression[] args) throws XPathSyntaxException {
        super(name, args, -1, true);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        evaluateArguments(model, evalContext);

        Hashtable<String, IFunctionHandler> funcHandlers = evalContext.getFunctionHandlers();

        IFunctionHandler handler = funcHandlers.get(id);
        if (handler != null) {
            return evalCustomFunction(handler, evaluatedArgs, evalContext);
        }

        throw new XPathUnhandledException("function \'" + id + "\'");
    }

    /**
     * Given a handler registered to handle the function, try to coerce the
     * function arguments into one of the prototypes defined by the handler. If
     * no suitable prototype found, throw an eval exception. Otherwise,
     * evaluate.
     *
     * Note that if the handler supports 'raw args', it will receive the full,
     * unaltered argument list if no prototype matches. (this lets functions
     * support variable-length argument lists)
     */
    private static Object evalCustomFunction(IFunctionHandler handler, Object[] args,
                                             EvaluationContext ec) {
        Vector prototypes = handler.getPrototypes();
        Enumeration e = prototypes.elements();
        Object[] typedArgs = null;

        boolean argPrototypeArityMatch = false;
        Class[] proto;
        while (typedArgs == null && e.hasMoreElements()) {
            // try to coerce args into prototype, stopping on first success
            proto = (Class[])e.nextElement();
            typedArgs = matchPrototype(args, proto);
            argPrototypeArityMatch = argPrototypeArityMatch ||
                    (proto.length == args.length);
        }

        if (typedArgs != null) {
            return handler.eval(typedArgs, ec);
        } else if (handler.rawArgs()) {
            // should we have support for expanding nodesets here?
            return handler.eval(args, ec);
        } else if (!argPrototypeArityMatch) {
            // When the argument count doesn't match any of the prototype
            // sizes, we have an arity error.
            throw new XPathArityException(handler.getName(),
                    "a different number of arguments",
                    args.length);
        } else {
            throw new XPathTypeMismatchException("for function \'" +
                    handler.getName() + "\'");
        }
    }

    /**
     * Given a prototype defined by the function handler, attempt to coerce the
     * function arguments to match that prototype (checking # args, type
     * conversion, etc.). If it is coercible, return the type-converted
     * argument list -- these will be the arguments used to evaluate the
     * function.  If not coercible, return null.
     */
    private static Object[] matchPrototype(Object[] args, Class[] prototype) {
        Object[] typed = null;

        if (prototype.length == args.length) {
            typed = new Object[args.length];

            for (int i = 0; i < prototype.length; i++) {
                typed[i] = null;

                // how to handle type conversions of custom types?
                if (prototype[i].isAssignableFrom(args[i].getClass())) {
                    typed[i] = args[i];
                } else {
                    try {
                        if (prototype[i] == Boolean.class) {
                            typed[i] = toBoolean(args[i]);
                        } else if (prototype[i] == Double.class) {
                            typed[i] = toNumeric(args[i]);
                        } else if (prototype[i] == String.class) {
                            typed[i] = toString(args[i]);
                        } else if (prototype[i] == Date.class) {
                            typed[i] = toDate(args[i]);
                        }
                    } catch (XPathTypeMismatchException xptme) {
                    }
                }

                if (typed[i] == null) {
                    return null;
                }
            }
        }

        return typed;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        super.readExternal(in, pf);

        id = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);

        ExtUtil.writeString(out, id);
    }
}
