KAM Navigator Plugin
==============================

This Cytoscape Plugin allows users to access and query BEL Framework Knowledge Assembly Models (KAMs) using the BEL Framework Web Services API.

License
-------
This project is licensed under the terms of the [LGPL v3](http://www.gnu.org/licenses/lgpl.txt).

Dependencies
----------
-   BELFramework Webservice Plugin V0.9

Building
--------

To build this plugin you will need

-   [ANT](http://ant.apache.org/)
-   BELFramework Webservice Plugin
-   [OpenBEL Framework](https://github.com/OpenBEL/openbel-framework/) 2.0.0 or greater installed
-   [Cytoscape](http://www.cytoscape.org/) 2.8.x installed

Make sure the latest BEL Framework Web Service Plugin is built and copied to the `lib/` folder.

Once installed you will need to configure the *HOME* location of each

-   Uncomment and set the `BELFRAMEWORK_HOME` property in build.properties to
    point to the BELFramework installation folder.
-   Uncomment and set the `CYTOSCAPE_HOME` property in build.properties to point
    to the Cytoscape installation folder.

To build the project use the following commands

-   `ant package`

    Builds the plugin ready to install into Cytoscape.

-   `ant deploy`

    Packages the plugin and copies it to the plugins folder of your Cytoscape installation.

    *Important* - Also make sure the BELFramework Web Service Plugin is deployed as a cytoscape plugin.


Setting up Eclipse
------------------

To set up the [Eclipse IDE](http://www.eclipse.org/) for working with this plugin

-   Check out the project

    `git clone https://github.com/OpenBEL/cytoscape-plugins.git`

-   In Eclipse go to *File -> New -> Java Project* and uncheck *Use default location*.

-   Name your project org.openbel.cytoscape.navigator

-   Enter the location of your local org.openbel.cytoscape.navigator folder.

    `/path/to/git/clone/cytoscape-plugins/org.openbel.cytoscape.navigator`

-   Hit Ok and a new project will be created.

-   Configure classpath

    -   Add `org.openbel.cytsocape.webservice-0.9.jar` to the project classpath.
    -   Add `org.openbel.framework.ws.model-2.0.0.jar` (or later version) from BELFRAMEWORK_HOME/lib/webapiclient to the project classpath.
    -   Add `cytoscape.jar` from CYTOSCAPE_HOME/ to the project classpath.

-   (optional) To build with ANT drag the build.xml to the ANT view.
