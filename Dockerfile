FROM urbanetic/bimserver
MAINTAINER Oliver Lade <oliver.lade@unimelb.edu.au>

# TODO(orlade): Replace download with JAR built by Maven from added source code.
ADD https://github.com/urbanetic/geotree-bimserver-plugin/releases/download/0.0.1/geotree-bimserver-plugin-0.0.1-SNAPSHOT.jar $BIMSERVER_APP/WEB-INF/plugins/geotree-bimserver-plugin.jar
