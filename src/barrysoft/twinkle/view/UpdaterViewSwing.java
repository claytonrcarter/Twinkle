package barrysoft.twinkle.view;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import barrysoft.gui.GUIEventsDispatcher;
import barrysoft.twinkle.UpdateException;
import barrysoft.twinkle.UpdateRequest;
import barrysoft.twinkle.UpdateVersion;
import barrysoft.twinkle.view.gui.UpdateAvailableDialog;
import barrysoft.twinkle.view.gui.UpdateProgressDialog;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Swing implementation of an {@link UpdateView}.
 * 
 * @author Daniele Rapagnani
 */

public class UpdaterViewSwing implements UpdaterView 
{		
	private final Vector<UpdaterViewObserver> observers =
		new Vector<UpdaterViewObserver>();
	
	private final GUIEventsDispatcher<UpdaterEventType> dispatcher =
		new GUIEventsDispatcher<UpdaterEventType>();

	private final UpdateAvailableDialog 	updateAvailableDialog;
	private final UpdateProgressDialog 		updateProgressDialog;
	
	private final Preferences				updatePrefs;

	private final Action installAction = new AbstractAction() 
	{
		private static final long serialVersionUID = -7773489410353949311L;

		{
			putValue(Action.NAME, "Install");
		}
		
		public void actionPerformed(ActionEvent e)
		{
			updatePrefs.putBoolean("updater.auto",
					updateAvailableDialog.isAlwaysDownload());
			
			fireUpdateRequested(updateAvailableDialog.getUpdateVersion(),
					updateAvailableDialog.getUpdateSource());
		}
	};
	
	private final Action downloadAction = new AbstractAction()
	{
		private static final long serialVersionUID = -7773489410353949311L;

		{
			putValue(Action.NAME, "Download");
		}

		public void actionPerformed(ActionEvent e)
		{
			updatePrefs.putBoolean("updater.auto",
					updateAvailableDialog.isAlwaysDownload());

			fireDownloadRequested( updateAvailableDialog.getUpdateVersion(),
				  	       updateAvailableDialog.getUpdateSource());
		}
	};

	private final Action skipVersionAction = new AbstractAction() 
	{
		private static final long serialVersionUID = 0L;

		{
			putValue(Action.NAME, "Skip this version");
		}

		public void actionPerformed(ActionEvent e)
		{
			updatePrefs.put("updater.skipversion",
					updateAvailableDialog.getUpdateVersion()
                                                             .getVersion() );
		}
	};
	
	private final Action cancelUpdateAction = new AbstractAction() 
	{
		private static final long serialVersionUID = -7773489410353949311L;

		{
			putValue(Action.NAME, "Cancel");
		}
		
		public void actionPerformed(ActionEvent e)
		{
			fireCancelUpdate();
		}
	};
	
	public UpdaterViewSwing(Preferences updatePreferences)
	{
		this.updatePrefs = updatePreferences;
                
                if ( updatePrefs.getBoolean( "updater.downloadonly", false ) )
                  this.updateAvailableDialog =
                          new UpdateAvailableDialog( downloadAction,
                                                     skipVersionAction );
                else
		this.updateAvailableDialog = new UpdateAvailableDialog(installAction, skipVersionAction);
		this.updateProgressDialog = new UpdateProgressDialog(cancelUpdateAction);
		
		if (!updatePrefs.getBoolean("updater.auto", false))
			dispatcher.addTarget(updateAvailableDialog);
		
		dispatcher.addTarget(updateProgressDialog);
	}

	public void downloadCompleted(UpdateVersion version)
	{
		dispatcher.dispatch(UpdaterEventType.DOWNLOAD_ENDED);
	}

	public void downloadProgress(UpdateVersion version, int bytesLoaded)
	{
		dispatcher.dispatch(UpdaterEventType.DOWNLOAD_PROGRESS, bytesLoaded);
	}

	public void downloadStarted(UpdateVersion version)
	{		
		dispatcher.dispatch(UpdaterEventType.DOWNLOAD_STARTED);
	}

	public void newVersionFound(UpdateVersion version, UpdateRequest source)
	{
		String skipVersion = updatePrefs.get("updater.skipversion", "");
		
		if (!skipVersion.isEmpty() && skipVersion.equals(version.getVersion())) 
			noUpdateRequired();
		else if (!updatePrefs.getBoolean("updater.auto", false))
			dispatcher.dispatch(UpdaterEventType.NEW_VERSION_FOUND, version, source);
		else
			fireUpdateRequested(version, source);
	}

	public void noUpdateRequired()
	{
		dispatcher.dispatch(UpdaterEventType.UPDATE_COMPLETED);
	}

	public void updateCanceled()
	{
		dispatcher.dispatch(UpdaterEventType.DOWNLOAD_ENDED);
	}
	
	public void addObserver(UpdaterViewObserver observer)
	{
		observers.add(observer);
	}

	public void removeObserver(UpdaterViewObserver observer)
	{
		observers.add(observer);
	}
	
	protected void fireUpdateRequested(UpdateVersion version, UpdateRequest source)
	{
		for	(UpdaterViewObserver observer : observers)
			observer.updateRequested(version, source);
	}
	
	protected void fireDownloadRequested(UpdateVersion version, UpdateRequest source)
	{
		for	(UpdaterViewObserver observer : observers)
			observer.downloadRequested(version, source);
	}

	protected void fireCancelUpdate()
	{
		for	(UpdaterViewObserver observer : observers)
			observer.userCanceledUpdate();
	}
	
	protected void fireRestartRequested(UpdateRequest source)
	{
		for	(UpdaterViewObserver observer : observers)
			observer.restartRequested(source);
	}
	
	public void extractionEnded(File archiveFile)
	{
		dispatcher.dispatch(UpdaterEventType.EXTRACTION_ENDED);
	}

	public void extractionStarted(File archiveFile)
	{
		dispatcher.dispatch(UpdaterEventType.EXTRACTION_STARTED);
	}

	public void validationEnded(UpdateVersion version, File archiveFile)
	{
		dispatcher.dispatch(UpdaterEventType.VALIDATION_ENDED);
	}

	public void validationStarted(UpdateVersion version, File archiveFile)
	{
		dispatcher.dispatch(UpdaterEventType.VALIDATION_STARTED);
	}

	public void updateError(UpdateException e)
	{
		dispatcher.dispatch(UpdaterEventType.ERROR_OCCURRED, e);
		
		JOptionPane.showMessageDialog(null, e.getMessage(), 
				"Error while updating", JOptionPane.ERROR_MESSAGE);
	}
	
	public void updateCompleted()
	{
		dispatcher.dispatch(UpdaterEventType.UPDATE_COMPLETED);
                try {
                  updatePrefs.flush();
                } catch ( BackingStoreException e ) {
                  // TODO handle this exception?
                }
	}

	public void checkingStarted(UpdateRequest source)
	{
		dispatcher.dispatch(UpdaterEventType.CHECKING_COMPLETED, source);
	}

	public void checkingEnded(UpdateRequest source)
	{
		dispatcher.dispatch(UpdaterEventType.CHECKING_UPDATES, source);
	}

	public void restartRequired(UpdateRequest source)
	{
		if (askForRestart())
			fireRestartRequested(source);
	}
	
	protected boolean askForRestart()
	{
		return (JOptionPane.showConfirmDialog(null, 
				"To complete the update process the application must be restarted.\n"+
				"Do you want to restart it now?",
				"Restart Required", JOptionPane.YES_NO_OPTION) == 0);
	}
}
