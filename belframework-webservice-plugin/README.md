BELFramework Webservice Plugin
==============================

This cytoscape plugin provides access to the webservice client stubs for the BELFramework Web API.

License
-------
This project is licensed under the terms of the [GPL v3](http://www.gnu.org/licenses/gpl-3.0.txt).

Building
--------

To build this plugin you will need

-   [ANT](http://ant.apache.org/)
-   BEL Framework 1.2.2 or greater installed
-   Cytoscape 2.7.x or 2.8.x installed

Once installed you will need to configure the *HOME* location of each

-   Change the `BELFRAMEWORK_HOME` property in build.properties to point to the BEL Framework installation folder.
-   Change the `CYTOSCAPE_HOME` property in build.properties to poi nt to the Cytoscape installation folder.

To build the project use the following commands

-   `ant package`

    Builds the plugin ready to install into Cytoscape.

-   `ant deploy`

    Packages the plugin and copies to the plugins folder of your Cytoscape installation.


Setting up Eclipse
------------------

To set up the [Eclipse IDE](http://www.eclipse.org/) for developing this plugin

-   Check out the [belframework project](https://belframework-org@github.com/belframework-org/belframework.git)

    `git clone https://belframework-org@github.com/belframework-org/belframework.git`

-   In Eclipse go to *File -> New -> Java Project* and uncheck *Use default location*.

-   Name your project belframework-webservice-plugin.

-   Enter the location of your local belframework-webservice-plugin folder.

    `/path/to/git/clone/belframework/belframework-webservice-plugin`

-   Hit Ok and a new project will be created.

-   Configure classpath

    -   Add `BELFrameworkWebAPIClient-1.2.2.jar` (or later version) from BELFRAMEWORK_HOME/lib/webapiclient to your classpath.
    -   Add `cytoscape.jar` from CYTOSCAPE_HOME/ to your classpath.

-   (optional) To build with ANT drag the build.xml to the ANT view.
