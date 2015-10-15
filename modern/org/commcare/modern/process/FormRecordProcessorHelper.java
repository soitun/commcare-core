package org.commcare.modern.process;

import org.commcare.core.interfaces.AbstractUserSandbox;
import org.commcare.core.process.XmlFormRecordProcessor;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 *  * Convenience methods, mostly for touchforms so we don't have to deal with Java IO
 * in Jython which is terrible
 *
 * Created by wpride1 on 8/20/15.
 */
public class FormRecordProcessorHelper extends XmlFormRecordProcessor {
    public static void processXML(AbstractUserSandbox sandbox, String fileText) throws IOException, XmlPullParserException, UnfullfilledRequirementsException, InvalidStructureException {
        InputStream stream = new ByteArrayInputStream(fileText.getBytes(StandardCharsets.UTF_8));
        process(sandbox, stream);
    }

    public static void processFile(AbstractUserSandbox sandbox, File record) throws IOException, XmlPullParserException, UnfullfilledRequirementsException, InvalidStructureException {
        InputStream stream = new FileInputStream(record);
        process(sandbox, stream);
    }
}
