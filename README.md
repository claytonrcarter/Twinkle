Twinkle
=======

A Sparkle inspired update framework for Java.

In order to add automatic updates to a project with Twinkle, all you have to do is generate a Sparkle-style appcast XML file and include this in your app:

    Twinkle.getInstance()
           .runUpdate( MyApp.class,
                       "http://host.com/path/to/appcast.xml",
                       "/twinkle.properties" );

Twinkle can perform the update in-place, or can be configured to launch a URL to download the update.
