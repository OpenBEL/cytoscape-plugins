KAM Navigator Plugin
==============================

This Cytoscape Plugin allows users to access and query BEL Framework Knowledge Assembly Models (KAMs) using the BEL Framework Web Services API.

License
-------
This project is licensed under the terms of the [LGPL v3](http://www.gnu.org/licenses/lgpl.txt).

Dependencies
----------
-   BELFramework Webservice Plugin V0.8

Building
--------

To build this plugin you will need

-   [ANT](http://ant.apache.org/)
-   BELFramework Webservice Plugin
-   BELFramework 1.2.2 or greater installed
-   Cytoscape 2.8.x installed

Make sure the latest BEL Framework Web Service Plugin is built and copied to the `lib/` folder.

Once installed you will need to configure the *HOME* location of each

-   Change the `BELFRAMEWORK_HOME` property in build.properties to point to the BELFramework installation folder.
-   Change the `CYTOSCAPE_HOME` property in build.properties to poi nt to the Cytoscape installation folder.

To build the project use the following commands

-   `ant package`

    Builds the plugin ready to install into Cytoscape.

-   `ant deploy`

    Packages the plugin and copies it to the plugins folder of your Cytoscape installation.

    *Important* - Also make sure the BELFramework Web Service Plugin is deployed as a cytoscape plugin.


Setting up Eclipse
------------------

To set up the [Eclipse IDE](http://www.eclipse.org/) for working with this plugin

-   Check out the [belframework project](https://belframework-org@github.com/belframework-org/belframework.git)

    `git clone https://belframework-org@github.com/belframework-org/belframework.git`

-   In Eclipse go to *File -> New -> Java Project* and uncheck *Use default location*.

-   Name your project kam-navigator-plugin.

-   Enter the location of your local kam-navigator-plugin folder.

    `/path/to/git/clone/belframework/kam-navigator-plugin`

-   Hit Ok and a new project will be created.

-   Configure classpath

    -   Add `lib/belframework_webservice_0.8.jar` to the project classpath.
    -   Add `BELFrameworkWebAPIClient-1.2.2.jar` (or later version) from BELFRAMEWORK_HOME/lib/webapiclient to the project classpath.
    -   Add `cytoscape.jar` from CYTOSCAPE_HOME/ to the project classpath.

-   (optional) To build with ANT drag the build.xml to the ANT view.
