# Geotree BIMserver Plugin

[![Build Status](https://travis-ci.org/urbanetic/geotree-bimserver-plugin.svg)](https://travis-ci.org/urbanetic/geotree-bimserver-plugin)
[![Javadoc Status](https://img.shields.io/badge/Javadoc-latest-brightgreen.svg)](http://javadocs.geotree.urbanetic.net/)


The *Geotree* plugin for [BIMserver][bimserver] serialises IFC BIM models into a JSON document
containing both the semantic hierarchy and geometry of the model. This enables the [AURIN Asset
Conversion Service (ACS)][acs] to convert IFC files into entities compatible with [Atlas][atlas].

The [Docker image][dockerhub] can be used to easily deploy a BIMserver instance with the plugin
already installed. For more details on running the BIMserver image, see the [docker-bimserver][repo]
repo.


## Installation

To install the plugin:

1. [Install BIMserver][install] as you normally would
2. Compile the [Geotree source][geotree] to a JAR with `mvn package` (or see [releases][releases])
3. Copy the compiled JAR to the `plugins` folder in the directory to which you installed BIMserver
4. Restart the BIMserver service

To build docker from the parent directory:

1. docker build --rm -t urbanetic/geotree-bimserver:latest -f docker/Dockerfile .

If the plugin was instaled successfully, you should be able to see a plugin called
`JsonIfcGeometryTreeSerializerPlugin` listed in the [Plugins section of the admin view][plugins].

Note that to compile, you will need to specify a Maven repository to provide the BIMserver
dependencies. The relevant JARs are in the [`lib` archive of the official releases][lib]. Official
Maven support for the BIMserver libraries is an [open issue][issue].


[acs]: https://github.com/urbanetic/aurin-acs
[atlas]: https://github.com/urbanetic/atlas
[bimserver]: http://bimserver.org/
[dockerhub]: https://registry.hub.docker.com/u/urbanetic/geotree-bimserver/
[geotree]: https://github.com/urbanetic/geotree-bimserver-plugin
[install]: https://github.com/opensourceBIM/BIMserver/wiki/Get-Started-Quick-Guide
[issue]: https://github.com/opensourceBIM/BIMserver/issues/143
[plugins]: http://localhost:8082/admin/?page=ServerSettings&subpage=Plugins
[lib]: https://github.com/opensourceBIM/BIMserver/releases/download/1.3.0-FINAL-2014-04-25/bimserver-lib-1.3.0-FINAL-2014-04-25.zip
[releases]: https://github.com/urbanetic/geotree-bimserver-plugin/releases
[repo]: https://github.com/urbanetic/docker-bimserver
