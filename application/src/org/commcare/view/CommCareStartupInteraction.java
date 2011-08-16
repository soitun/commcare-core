/**
 * 
 */
package org.commcare.view;

import org.commcare.util.YesNoListener;
import org.javarosa.core.services.locale.Localization;

import de.enough.polish.ui.Command;
import de.enough.polish.ui.CommandListener;
import de.enough.polish.ui.Displayable;
import de.enough.polish.ui.Form;
import de.enough.polish.ui.Gauge;
import de.enough.polish.ui.StringItem;

/**
 * @author ctsims
 *
 */
public class CommCareStartupInteraction extends Form implements CommandListener {
	private StringItem messageItem;
	
	private YesNoListener listener;
	
	private Command yes;
	private Command no;
	
	private Gauge gauge;

	public CommCareStartupInteraction(String message) {
		super(failSafeText("intro.title", "CommCare"));
		messageItem = new StringItem(null, null);
		//#style loadingGauge?
		gauge = new Gauge(null, false, Gauge.INDEFINITE,Gauge.CONTINUOUS_RUNNING);
		this.append(messageItem);
		this.append(gauge);
		gauge.setVisible(false);
		this.setCommandListener(this);
		setMessage(message, true);
	}
	
	public void setMessage(String message, boolean showSpinner) {
		this.messageItem.setText(message);
		gauge.setVisible(showSpinner);
	}
	
	public void AskYesNo(String message, YesNoListener listener) {		
		setMessage(message, false);
		this.listener = listener;
		if(yes == null) {
			yes = new Command(failSafeText("yes","Yes"), Command.OK, 0);
			no = new Command(failSafeText("no","No"), Command.CANCEL, 0);
			
			this.addCommand(yes);
			this.addCommand(no);
		}
		
	}
	
	public static String failSafeText(String localeId, String fallback) {
		try { 
			return Localization.get(localeId);
		} catch(Exception e) {
			return fallback;
		}
	}

	public void commandAction(Command c, Displayable d) {
		if(c.equals(yes)) {
			listener.yes();
		} else if(c.equals(no)) {
			listener.no();
		}
	}
}
