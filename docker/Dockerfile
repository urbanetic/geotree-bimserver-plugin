FROM urbanetic/bimserver
MAINTAINER Oliver Lade <oliver.lade@unimelb.edu.au>

# Add the packaged JAR file into the image.
ADD target/geotree-bimserver-plugin.jar $BIMSERVER_APP/WEB-INF/plugins/geotree-bimserver-plugin.jar

# Start the BIMserver process and send a request to perform the initial admin setup.
# Admin username is "admin@bimserver.org", and password is "admin".
# TODO(orlade): Do this more directly by writing to database.
# The 30 second sleep is required, since starting Tomcat synchronously doesn't yield once started.
RUN  $CATALINA_HOME/bin/startup.sh && \
    echo "Waiting 30 secs for BIMserver to initialize..." && \
    sleep 30 && \
    curl -X POST localhost:8080/bimserver/json -d '{"request":{"interface":"org.bimserver.AdminInterface","method":"setup","parameters":{"siteAddress":"http://localhost:8888/bimserver","smtpServer":"bimserver.org","smtpSender":"admin@bimserver.org","adminName":"Administrator","adminUsername":"admin@bimserver.org","adminPassword":"admin"}}}'
