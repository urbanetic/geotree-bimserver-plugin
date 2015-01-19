# Geotree BIMserver Plugin

The Geotree plugin for [BIMserver][bimserver] serialises IFC BIM models into a JSON document
containing both the semantic hierarchy and geometry of the model. This enables the [AURIN Asset
Conversion Service (ACS)][acs] to convert IFC files into entities compatible with [Atlas][atlas].

## Installation

To install the plugin:

1. [Install BIMserver][install] as you normally would
2. Compile the [Geotree source][geotree] to a JAR with `mvn package`
3. Copy the compiled JAR to the `plugins` folder in the directory to which you installed BIMserver
4. Restart the BIMserver service

If the plugin was instaled successfully, you should be able to see a plugin called
`JsonIfcGeometryTreeSerializerPlugin` listed in the [Plugins section of the admin view][plugins].

Note that to compile, you will need to specify a Maven repository to provide the BIMserver
dependencies. The relevant JARs are in the [`lib` archive of the official releases][lib]. Official
Maven support for the BIMserver libraries is an [open issue][issue].


[bimserver]: http://bimserver.org/
[acs]: https://github.com/urbanetic/aurin-acs
[atlas]: https://github.com/urbanetic/atlas
[install]: https://github.com/opensourceBIM/BIMserver/wiki/Get-Started-Quick-Guide
[geotree]: https://github.com/urbanetic/geotree-bimserver-plugin
[plugins]: http://localhost:8082/admin/?page=ServerSettings&subpage=Plugins
[lib]: https://github.com/opensourceBIM/BIMserver/releases/download/1.3.0-FINAL-2014-04-25/bimserver-lib-1.3.0-FINAL-2014-04-25.zip
[issue]: https://github.com/opensourceBIM/BIMserver/issues/143
