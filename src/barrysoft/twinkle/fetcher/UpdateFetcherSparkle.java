package barrysoft.twinkle.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import barrysoft.twinkle.UpdateException;
import barrysoft.twinkle.UpdateVersion;
import barrysoft.twinkle.fetcher.sparkle.SparkleEnclosure;
import barrysoft.twinkle.fetcher.sparkle.SparkleEntry;
import barrysoft.twinkle.fetcher.sparkle.SparkleModule;
import barrysoft.twinkle.fetcher.sparkle.SparkleModuleImpl;
import com.sun.syndication.feed.module.Module;

import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * This {@link UpdateFetcher} implementation fetches and parses
 * <a href="http://sparkle.andymatuschak.org/">Sparkle</a> 
 * {@code App-Cast} RSS feeds using a custom ROME module 
 * ({@link SparkleModuleImpl})
 * 
 * @author Daniele Rapagnani
 */

public class UpdateFetcherSparkle implements UpdateFetcher 
{
	private static final UpdateFetcherSparkle instance = new UpdateFetcherSparkle();
	
	public static UpdateFetcherSparkle getInstance()
	{
		return instance;
	}
	
	private UpdateFetcherSparkle()
	{
		
	}
	
	public List<UpdateVersion> fetchVersions(URL from) throws UpdateException
	{
		Vector<UpdateVersion> operations = 
			new Vector<UpdateVersion>();
		
		SyndFeed feed = fetchFeedFromURL(from);
		
		for (Object e : feed.getEntries()) 
		{
			SyndEntry entry = (SyndEntry)e;

			operations.addAll(convertSparkleEntry(entry));
		}
		
		return operations;
	}
	
	protected SyndFeed fetchFeedFromURL(URL feedUrl) throws UpdateException
	{
		SyndFeedInput sfi = new SyndFeedInput();
		SyndFeed feed;
		
		try {
                  
                  String userAgent = "Java/" +
                                     System.getProperty("java.version") +
                                     " ( " + System.getProperty("os.name") +
                                     " " + System.getProperty("os.version") +
                                     " )";

                  URLConnection uConn = feedUrl.openConnection();
                  uConn.setRequestProperty("User-Agent", userAgent );

			feed = sfi.build(new XmlReader(uConn));
		} catch (IllegalArgumentException e) {
			throw new UpdateException("Unknown type of update feed", e);
		} catch (FeedException e) {
			throw new UpdateException("Error while parsing update feed", e);
		} catch (IOException e) {
			throw new UpdateException("Can't fetch update feed", e);
		}
		
		return feed;
	}
	
	protected List<UpdateVersion> convertSparkleEntry(SyndEntry entry)
		throws UpdateException
	{

                List<UpdateVersion> uvs = new ArrayList<UpdateVersion>();

                // convert the entry to a Sparkle entry
		SparkleEntry spk = (SparkleEntry)entry.getModule(SparkleModule.URI);


                for ( int i = 0; i < entry.getEnclosures().size(); i++ ) {

                  UpdateVersion op = new UpdateVersion();

                  op.setName(entry.getTitle());
                  op.setDate(entry.getPublishedDate());

                  if ( spk != null )
                    op.setMinimumSystemVersion(spk.getMinimumSystemVersion());

                  if (entry.getDescription() != null)
                    op.setDescription(entry.getDescription().getValue());

                  try {
                    //TODO this should be OK if they don't enter a
                    // release notes URL ... but it's not
                          op.setReleaseNotesLink(new URL(spk.getReleaseNotesLink()));
                  } catch (MalformedURLException e) {
                          throw new UpdateException("Can't parse release note URL", e);
                  } catch ( NullPointerException e ) {}

                  convertSparkleEnclosures( entry, op, i );

                  uvs.add(op);

                }
		
		return uvs;
	}

        /**
         * Only converts the first enclosure.
         * @deprecated
         * @param entry
         * @param targetOperation
         * @throws UpdateException
         */
	protected void convertSparkleEnclosures(SyndEntry entry, UpdateVersion targetOperation)
		throws UpdateException
	{
          convertSparkleEnclosures(entry, targetOperation, 0);
        }

	protected void convertSparkleEnclosures( SyndEntry entry,
                                                UpdateVersion targetOperation,
                                                int enclosureIndex )
		throws UpdateException
	{
		SparkleEntry spk = (SparkleEntry)entry.getModule(SparkleModule.URI);

		if (entry.getEnclosures().isEmpty())
		{
			Logger.getLogger(getClass()).debug("No enclosure was specified for this " +
					"AppCast, this is probably an error!");
			
			return;
		}
		
		SyndEnclosure enclosure = (SyndEnclosure)entry.getEnclosures().get(enclosureIndex);
		SparkleEnclosure senclosure = spk.getEnclosures().get(enclosureIndex);
		
		try {
			targetOperation.setDownloadUrl(new URL(enclosure.getUrl()));
		} catch (MalformedURLException e1) {
			throw new UpdateException("Can't parse download url", e1);
		}
		
		targetOperation.setDownloadSize(enclosure.getLength());
		targetOperation.setDsaSignature(senclosure.getDsaSignature());
		targetOperation.setMd5Sum(senclosure.getMd5Sum());
		targetOperation.setVersion(senclosure.getVersion());
		targetOperation.setShortVersion(senclosure.getShortVersionString());
		targetOperation.setOS(senclosure.getOS());

	}
}
