package au.com.mutopia.plugin.deserializer;

import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginException;
import org.bimserver.plugins.PluginManager;
import org.bimserver.plugins.deserializers.DeserializerPlugin;
import org.bimserver.plugins.deserializers.EmfDeserializer;

public class JsonIfcGeometryTreeDeserializerPlugin implements DeserializerPlugin {

  private boolean initialized = false;

  @Override
  public EmfDeserializer createDeserializer(PluginConfiguration pluginConfiguration) {
    return new JsonIfcGeometryTreeDeserializer();
  }

  @Override
  public String getDescription() {
    return "JsonIfcGeometryTreeDeserializer";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public void init(PluginManager pluginManager) throws PluginException {
    initialized = true;
    pluginManager.requireSchemaDefinition();
  }

  @Override
  public boolean canHandleExtension(String extension) {
    return extension.equalsIgnoreCase("json");
  }

  @Override
  public boolean isInitialized() {
    return initialized;
  }

  @Override
  public String getDefaultName() {
    return "JsonIfcGeometryTreeDeserializer";
  }

  @Override
  public ObjectDefinition getSettingsDefinition() {
    return null;
  }
}
