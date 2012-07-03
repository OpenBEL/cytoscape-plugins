#!/usr/bin/env bash
# simple shell script to build (or rebuild) all projects

cd org.openbel.cytoscape.webservice/
ant
cd ../org.openbel.cytoscape.navigator/
ant
