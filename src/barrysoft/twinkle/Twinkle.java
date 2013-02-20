package barrysoft.twinkle;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;

import barrysoft.application.ApplicationInfo;
import barrysoft.options.Options;
import barrysoft.resources.ResourcesManager;
import barrysoft.twinkle.UpdateRequest.VersionType;
import barrysoft.twinkle.view.UpdaterView;
import barrysoft.twinkle.view.UpdaterViewSwing;

public class Twinkle 
{
	private static final Twinkle instance = new Twinkle();
	
	public static Twinkle getInstance()
	{
		return instance;
	}

        public void runUpdate(Class<?> main, String appcastUrl, String appinfoUrl)
	{
          runUpdate(main, appcastUrl, appinfoUrl, false);
        }

	/**
	 * Helper method to quickly start the update process.
	 * 
	 * @param main The class containing the main method.
	 * 				If after the update restarting is needed,
	 * 				this will be the class to be executed.
	 * 
	 * @param appcastUrl The url to the appcast feed
	 * 
	 * @param appinfoUrl Url to the property file holding the
	 * 						application infos.
	 */
	
	//TODO: It's just a quick hack for now
        //TODO I don't like the addition of the boolean download only
        // would be much cleaner if we just set the updater properties/options
        // outside of this and let the system pick them up??
	public void runUpdate( Class<?> main,
                               String appcastUrl,
                               String appinfoUrl,
                               boolean downloadOnly )
	{
		//Initialize the updater
		Updater.getInstance();
		
		ApplicationInfo info = new ApplicationInfo(main.getResourceAsStream(appinfoUrl));
		
		final UpdateRequest r;
		try
		{
			r = new UpdateRequest
			(
				VersionType.BUILD_NUMBER,
				info,
				main.getCanonicalName(),
				new URL(appcastUrl),
				ResourcesManager.getResources().getWorkingDirectory(),
				(File)null //TODO: Not using DSA Verification
			);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return;
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
			return;
		}

                Options opt = new Options();
                opt.setOption( "updater.downloadonly", downloadOnly );

		UpdaterView view = new UpdaterViewSwing(opt);
		final UpdaterController uc = new UpdaterController(Updater.getInstance(), view);

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				uc.checkUpdates(r);
			}
		}).start();
	}
}
