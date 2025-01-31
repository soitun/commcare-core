package org.commcare.cases.entity;


import org.commcare.cases.util.StringUtils;
import org.commcare.suite.model.DetailField;
import org.commcare.suite.model.DetailGroup;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.Logger;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.Closeable;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.annotation.Nullable;

/**
 * An AsyncEntity is an entity reference which is capable of building its
 * values (evaluating all Text elements/background data elements) lazily
 * rather than upfront when the entity is constructed.
 *
 * It is threadsafe.
 *
 * It will attempt to Cache its values persistently by a derived entity key rather
 * than evaluating them each time when possible. This can be slow to perform across
 * all entities internally due to the overhead of establishing the db connection, it
 * is recommended that the entities be primed externally with a bulk query.
 *
 * @author ctsims
 */
public class AsyncEntity extends Entity<TreeReference> {

    private final DetailField[] fields;
    private final Object[] data;
    private final String[] sortData;
    private final boolean[] relevancyData;
    private final String[][] sortDataPieces;

    private final String[] altTextData;
    private final EvaluationContext context;
    private final Hashtable<String, XPathExpression> mVariableDeclarations;
    private final DetailGroup mDetailGroup;

    private boolean mVariableContextLoaded = false;
    private final String mCacheIndex;
    private final String mDetailId;

    @Nullable
    private final EntityStorageCache mEntityStorageCache;

    /*
     * the Object's lock. NOTE: _DO NOT LOCK ANY CODE WHICH READS/WRITES THE CACHE
     * UNTIL YOU HAVE A LOCK FOR THE DB!
     * 
     * The lock is for the integrity of this object, not the larger environment, 
     * and any DB access has its own implict lock between threads, so it's easy
     * to accidentally deadlock if you don't already have the db lock
     * 
     * Basically you should never be calling mEntityStorageCache from inside of
     * a lock that 
     */
    private final Object mAsyncLock = new Object();

    public AsyncEntity(DetailField[] fields, EvaluationContext ec,
                       TreeReference t, Hashtable<String, XPathExpression> variables,
                       @Nullable EntityStorageCache cache, String cacheIndex, String detailId,
                       String extraKey, DetailGroup detailGroup) {
        super(t, extraKey);
        this.fields = fields;
        this.data = new Object[fields.length];
        this.sortData = new String[fields.length];
        this.sortDataPieces = new String[fields.length][];
        this.relevancyData = new boolean[fields.length];
        this.altTextData = new String[fields.length];
        this.context = ec;
        this.mVariableDeclarations = variables;
        this.mEntityStorageCache = cache;

        //TODO: It's weird that we pass this in, kind of, but the thing is that we don't want to figure out
        //if this ref is _cachable_ every time, since it's a pretty big lift
        this.mCacheIndex = cacheIndex;

        this.mDetailId = detailId;
        this.mDetailGroup = detailGroup;
    }

    private void loadVariableContext() {
        synchronized (mAsyncLock) {
            if (!mVariableContextLoaded) {
                //These are actually in an ordered hashtable, so we can't just get the keyset, since it's
                //in a 1.3 hashtable equivalent
                for (Enumeration<String> en = mVariableDeclarations.keys(); en.hasMoreElements(); ) {
                    String key = en.nextElement();
                    context.setVariable(key, FunctionUtils.unpack(mVariableDeclarations.get(key).eval(context)));
                }
                mVariableContextLoaded = true;
            }
        }
    }

    @Override
    public Object getField(int i) {
        synchronized (mAsyncLock) {
            loadVariableContext();
            if (data[i] == null) {
                try {
                    data[i] = fields[i].getTemplate().evaluate(context);
                } catch (XPathException xpe) {
                    Logger.exception("Error while evaluating field for case list ", xpe);
                    xpe.printStackTrace();
                    data[i] = "<invalid xpath: " + xpe.getMessage() + ">";
                }
            }
            return data[i];
        }
    }

    @Override
    public String getNormalizedField(int i) {
        String normalized = this.getSortField(i);
        if (normalized == null) {
            return "";
        }
        return normalized;
    }

    @Override
    public String getSortField(int i) {
        synchronized (mAsyncLock) {
            if (sortData[i] != null) {
                return sortData[i];
            }
        }
        try (Closeable ignored = mEntityStorageCache != null ? mEntityStorageCache.lockCache() : null) {
            //get our second lock.
            synchronized (mAsyncLock) {
                if (sortData[i] == null) {
                    // sort data not in search field cache; load and store it
                    Text sortText = fields[i].getSort();
                    if (sortText == null) {
                        return null;
                    }
                    String cacheKey = null;
                    if (mEntityStorageCache != null) {
                        cacheKey = mEntityStorageCache.getCacheKey(mDetailId, String.valueOf(i));
                        if (mCacheIndex != null) {
                            //Check the cache!
                            String value = mEntityStorageCache.retrieveCacheValue(mCacheIndex, cacheKey);
                            if (value != null) {
                                this.setSortData(i, value);
                                return sortData[i];
                            }

                            loadVariableContext();
                            try {
                                sortText = fields[i].getSort();
                                if (sortText == null) {
                                    this.setSortData(i, getFieldString(i));
                                } else {
                                    this.setSortData(i, StringUtils.normalize(sortText.evaluate(context)));
                                }
                                if (mEntityStorageCache != null) {
                                    mEntityStorageCache.cache(mCacheIndex, cacheKey, sortData[i]);
                                }
                            } catch (XPathException xpe) {
                                Logger.exception("Error while evaluating sort field", xpe);
                                xpe.printStackTrace();
                                sortData[i] = "<invalid xpath: " + xpe.getMessage() + ">";
                            }
                        }
                    }
                }
                return sortData[i];
            }
        } catch (IOException e) {
            Logger.exception("Error while getting sort field", e);
        }
        return null;
    }

    @Override
    public int getNumFields() {
        return fields.length;
    }

    @Override
    public boolean isValidField(int fieldIndex) {
        //NOTE: This totally jacks the asynchronicity. It's only used in
        //detail fields for now, so not super important, but worth bearing
        //in mind
        synchronized (mAsyncLock) {
            loadVariableContext();
            if (getField(fieldIndex).equals("")) {
                return false;
            }

            try {
                this.relevancyData[fieldIndex] = this.fields[fieldIndex].isRelevant(this.context);
            } catch (XPathSyntaxException e) {
                final String msg = "Invalid relevant condition for field : " + fields[fieldIndex].getHeader().toString();
                Logger.exception(msg, e);
                throw new RuntimeException(msg);
            }
            return this.relevancyData[fieldIndex];
        }
    }

    @Override
    public Object[] getData() {
        for (int i = 0; i < this.getNumFields(); ++i) {
            this.getField(i);
        }
        return data;
    }

    @Override
    public String[] getSortFieldPieces(int i) {
        if (getSortField(i) == null) {
            return new String[0];
        }
        return sortDataPieces[i];
    }

    private void setSortData(int i, String val) {
        synchronized (mAsyncLock) {
            this.sortData[i] = val;
            this.sortDataPieces[i] = breakUpField(val);
        }
    }

    public void setSortData(String cacheKey, String val) {
        if (mEntityStorageCache == null) {
            throw new IllegalStateException("No entity cache defined");
        }
        int sortIndex = mEntityStorageCache.getSortFieldIdFromCacheKey(mDetailId, cacheKey);
        if (sortIndex != -1) {
            setSortData(sortIndex, val);
        }
    }

    private static String[] breakUpField(String input) {
        if (input == null) {
            return new String[0];
        } else {
            //We always fuzzy match on the sort field and only if it is available
            //(as a way to restrict possible matching)
            return input.split("\\s+");
        }
    }

    @Nullable
    @Override
    public String getGroupKey() {
        if (mDetailGroup != null) {
            return  (String)mDetailGroup.getFunction().eval(context);
        }
        return null;
    }

    @Nullable
    public String getAltTextData(int i) {
        synchronized (mAsyncLock) {
            loadVariableContext();
            Text altText = fields[i].getAltText();
            if (altText != null) {
                try {
                    altTextData[i] = altText.evaluate(context);
                } catch (XPathException xpe) {
                    Logger.exception("Error while evaluating field for case list ", xpe);
                    xpe.printStackTrace();
                    altTextData[i] = "<invalid xpath: " + xpe.getMessage() + ">";
                }
            }
            return altTextData[i];
        }
    }

    @Override
    public String[] getAltText() {
        for (int i = 0; i < this.getNumFields(); ++i) {
            this.getAltTextData(i);
        }
        return altTextData;
    }
}
