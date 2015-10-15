package org.commcare.api.persistence;

import org.commcare.cases.ledger.Ledger;
import org.commcare.cases.model.Case;
import org.commcare.core.interfaces.AbstractUserSandbox;
import org.javarosa.core.model.User;
import org.javarosa.core.model.instance.FormInstance;

/**
 * A sandbox for user data using SqlIndexedStorageUtility. Sandbox is per-User
 *
 * @author wspride
 */
public class UserSqlSandbox extends AbstractUserSandbox {
    private final SqlIndexedStorageUtility<Case> caseStorage;
    private final SqlIndexedStorageUtility<Ledger> ledgerStorage;
    private final SqlIndexedStorageUtility<User> userStorage;
    private final SqlIndexedStorageUtility<FormInstance> userFixtureStorage;
    private final SqlIndexedStorageUtility<FormInstance> appFixtureStorage;
    private User user = null;

    /**
     * Create a sandbox of the necessary storage objects with the shared
     * factory.
     */
    public UserSqlSandbox(String username) {
        //we can't name this table "Case" becase that's reserved by sqlite
        caseStorage = new SqlIndexedStorageUtility<>(Case.class, username, "CCCase");
        ledgerStorage = new SqlIndexedStorageUtility<>(Ledger.class, username, "Ledger");
        userStorage = new SqlIndexedStorageUtility<>(User.class, username, "User");
        userFixtureStorage = new SqlIndexedStorageUtility<>(FormInstance.class, username, "UserFixture");
        appFixtureStorage = new SqlIndexedStorageUtility<>(FormInstance.class, username, "AppFixture");
    }


    public SqlIndexedStorageUtility<Case> getCaseStorage() {
        return caseStorage;
    }

    public SqlIndexedStorageUtility<Ledger> getLedgerStorage() {
        return ledgerStorage;
    }

    public SqlIndexedStorageUtility<User> getUserStorage() {
        return userStorage;
    }

    public SqlIndexedStorageUtility<FormInstance> getUserFixtureStorage() {
        return userFixtureStorage;
    }

    public SqlIndexedStorageUtility<FormInstance> getAppFixtureStorage() {
        return appFixtureStorage;
    }

    @Override
    public User getLoggedInUser() {
        if(user == null){
            SqlIndexedStorageUtility<User> userStorage = getUserStorage();
            JdbcSqlStorageIterator<User> iterator = userStorage.iterate();
            if(iterator.hasMore()){
                // should be only one user here
                user =  iterator.next();
            } else {
                return null;
            }
        }
        return user;
    }

    @Override
    public void setLoggedInUser(User user) {
        this.user = user;
    }

    //TODO WSP implement sync token stuff in next iteration, but useful to have in superclass now for AndroidSandbox
}
