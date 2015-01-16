package au.com.mutopia.plugin;

import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginException;
import org.bimserver.plugins.PluginManager;
import org.bimserver.plugins.serializers.AbstractSerializerPlugin;
import org.bimserver.plugins.serializers.EmfSerializer;

/**
 * Serializer plugin for BIMserver to convert a BIM model into a JSON document containing a
 * hierarchy of the IFC elements, their geometries, colors and parameters.
 */
public class JsonIfcGeometryTreeSerializerPlugin extends AbstractSerializerPlugin {

  private boolean initialized = false;
  private static final String VERSION = "1.0";

  @Override
  public String getDescription() {
    return "JsonIfcGeometryTreeSerializer";
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public void init(PluginManager pluginManager) throws PluginException {
    initialized = true;
  }

  @Override
  public boolean needsGeometry() {
    return true;
  }

  @Override
  public EmfSerializer createSerializer(PluginConfiguration pluginConfiguration) {
    return new JsonIfcGeometryTreeSerializer();
  }

  @Override
  public String getDefaultName() {
    return "JsonIfcGeometryTreeSerializer";
  }

  @Override
  public String getDefaultContentType() {
    return "application/json";
  }

  @Override
  public String getDefaultExtension() {
    return "json";
  }

  @Override
  public boolean isInitialized() {
    return initialized;
  }

  @Override
  public ObjectDefinition getSettingsDefinition() {
    return super.getSettingsDefinition();
  }
}
