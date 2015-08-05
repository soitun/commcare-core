package org.commcare.util.mocks;

import org.commcare.api.interfaces.UserDataInterface;
import org.commcare.api.util.UserDataUtils;
import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.cases.ledger.instance.LedgerInstanceTreeElement;
import org.commcare.suite.model.User;
import org.commcare.util.CommCarePlatform;
import org.commcare.util.CommCareSession;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;

/**
 * @author ctsims
 */
public class CommCareInstanceInitializer extends InstanceInitializationFactory {
    CommCareSession session;
    CaseInstanceTreeElement casebase;
    LedgerInstanceTreeElement stockbase;
    UserDataInterface mSandbox;
    CommCarePlatform mPlatform;
    
    public CommCareInstanceInitializer(MockUserDataSandbox sandbox, CommCarePlatform platform){ 
        this(null, sandbox, platform);
    }
    public CommCareInstanceInitializer(CommCareSession session, UserDataInterface sandbox, CommCarePlatform platform) {
        this.session = session;
        this.mSandbox = sandbox;
        this.mPlatform = platform;
    }

    public AbstractTreeElement generateRoot(ExternalDataInstance instance) {
        String ref = instance.getReference();
        if (ref.contains(LedgerInstanceTreeElement.MODEL_NAME)) {
            if (stockbase == null) {
                stockbase = new LedgerInstanceTreeElement(instance.getBase(), mSandbox.getLedgerStorage());
            } else {
                //re-use the existing model if it exists.
                stockbase.rebase(instance.getBase());
            }
            return stockbase;
        } else if (ref.contains(CaseInstanceTreeElement.MODEL_NAME)) {
            if (casebase == null) {
                casebase = new CaseInstanceTreeElement(instance.getBase(), mSandbox.getCaseStorage(), false);
            } else {
                //re-use the existing model if it exists.
                casebase.rebase(instance.getBase());
            }
            return casebase;
        } else if (instance.getReference().contains("fixture")) {
            //TODO: This is all just copied from J2ME code. that's pretty silly. unify that.
            String userId = "";
            User u = mSandbox.getLoggedInUser();

            if (u != null) {
                userId = u.getUniqueId();
            }

            String refId = ref.substring(ref.lastIndexOf('/') + 1, ref.length());
            try {
                FormInstance fixture = UserDataUtils.loadFixture(mSandbox, refId, userId);

                if (fixture == null) {
                    throw new RuntimeException("Could not find an appropriate fixture for src: " + ref);
                }

                TreeElement root = fixture.getRoot();
                root.setParent(instance.getBase());
                return root;

            } catch (IllegalStateException ise) {
                throw new RuntimeException("Could not load fixture for src: " + ref);
            }
        }
        if (instance.getReference().contains("session")) {
            User u = mSandbox.getLoggedInUser();
            TreeElement root = session.getSessionInstance("----", "CommCare CLI: " + mPlatform.getMajorVersion() + "." + mPlatform.getMinorVersion(), u.getUsername(), u.getUniqueId(), u.getProperties()).getRoot();
            root.setParent(instance.getBase());
            return root;
        }
        return null;
    }
}
