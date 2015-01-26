package au.com.mutopia.plugin;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.geometry.Matrix;
import org.bimserver.models.ifc2x3tc1.GeometryData;
import org.bimserver.models.ifc2x3tc1.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcAreaMeasure;
import org.bimserver.models.ifc2x3tc1.IfcBuilding;
import org.bimserver.models.ifc2x3tc1.IfcBuildingStorey;
import org.bimserver.models.ifc2x3tc1.IfcColourRgb;
import org.bimserver.models.ifc2x3tc1.IfcLengthMeasure;
import org.bimserver.models.ifc2x3tc1.IfcObject;
import org.bimserver.models.ifc2x3tc1.IfcObjectDefinition;
import org.bimserver.models.ifc2x3tc1.IfcPresentationStyleAssignment;
import org.bimserver.models.ifc2x3tc1.IfcPresentationStyleSelect;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcProductRepresentation;
import org.bimserver.models.ifc2x3tc1.IfcProject;
import org.bimserver.models.ifc2x3tc1.IfcProperty;
import org.bimserver.models.ifc2x3tc1.IfcPropertySet;
import org.bimserver.models.ifc2x3tc1.IfcPropertySetDefinition;
import org.bimserver.models.ifc2x3tc1.IfcPropertySingleValue;
import org.bimserver.models.ifc2x3tc1.IfcRelContainedInSpatialStructure;
import org.bimserver.models.ifc2x3tc1.IfcRelDecomposes;
import org.bimserver.models.ifc2x3tc1.IfcRelDefines;
import org.bimserver.models.ifc2x3tc1.IfcRelDefinesByProperties;
import org.bimserver.models.ifc2x3tc1.IfcRepresentation;
import org.bimserver.models.ifc2x3tc1.IfcRepresentationItem;
import org.bimserver.models.ifc2x3tc1.IfcSIUnit;
import org.bimserver.models.ifc2x3tc1.IfcSite;
import org.bimserver.models.ifc2x3tc1.IfcSpatialStructureElement;
import org.bimserver.models.ifc2x3tc1.IfcStyledItem;
import org.bimserver.models.ifc2x3tc1.IfcSurfaceStyle;
import org.bimserver.models.ifc2x3tc1.IfcSurfaceStyleElementSelect;
import org.bimserver.models.ifc2x3tc1.IfcSurfaceStyleRendering;
import org.bimserver.models.ifc2x3tc1.IfcUnit;
import org.bimserver.models.ifc2x3tc1.IfcUnitAssignment;
import org.bimserver.models.ifc2x3tc1.IfcUnitEnum;
import org.bimserver.models.ifc2x3tc1.IfcValue;
import org.bimserver.models.store.SIPrefix;
import org.bimserver.plugins.renderengine.RenderEngineException;
import org.bimserver.plugins.serializers.AbstractGeometrySerializer;
import org.bimserver.plugins.serializers.SerializerException;
import org.eclipse.emf.common.util.EList;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.stream.JsonWriter;

/**
 * Serializer for BimServer, to extract Ifc object hierarchy, color and parameters.
 */
public class JsonIfcGeometryTreeSerializer extends AbstractGeometrySerializer {
  public static final String AREA = "area";
  public static final String HEIGHT = "height";
  public static final String UNKNOWN_STYLE = "UNKNOWN";

  private static final Logger log = Logger.getLogger(JsonIfcGeometryTreeSerializer.class.getName());

  private final List<Long> surfaceStyleIds = Lists.newArrayList();
  private final HashMap<String, double[]> materialColorMap = Maps.newHashMap();

  private List<GeometryData> geometryDatas = Lists.newArrayList();
  private int sameGeometry = 0;

  private double lengthUnitConversion = 1.0; // Default to Meter;
  private double areaUnitConversion = 1.0; // Default to Square Meter;

  @Override
  public void reset() {
    surfaceStyleIds.clear();
    materialColorMap.clear();
    geometryDatas.clear();
    sameGeometry = 0;
    setMode(Mode.BODY);
  }

  @Override
  public boolean write(OutputStream out) throws SerializerException {
    if (getMode() == Mode.BODY) {
      OutputStreamWriter outputStreamWriter = new OutputStreamWriter(out, Charsets.UTF_8);
      JsonWriter jsonWriter = new JsonWriter(new BufferedWriter(outputStreamWriter));
      try {
        calculateLengthUnitConversion();
        getMaterialColorMap();
        writeIfcGeometryTree(jsonWriter);
        jsonWriter.flush();
      } catch (Exception e) {
        log.severe(e.getMessage());
      }
      setMode(Mode.FINISHED);
      return true;
    } else if (getMode() == Mode.FINISHED) {
      return false;
    }
    return false;
  }

  /**
   * Maps colors to their respective material surface styles.
   */
  private void getMaterialColorMap() {
    List<IfcSurfaceStyle> listSurfaceStyles = model.getAll(IfcSurfaceStyle.class);
    for (IfcSurfaceStyle ss : listSurfaceStyles) {
      EList<IfcSurfaceStyleElementSelect> styles = ss.getStyles();
      for (IfcSurfaceStyleElementSelect style : styles) {
        if (style instanceof IfcSurfaceStyleRendering) {
          IfcSurfaceStyleRendering ssr = (IfcSurfaceStyleRendering) style;
          IfcColourRgb colour = ssr.getSurfaceColour();
          if (!surfaceStyleIds.contains(ss.getOid())) {
            surfaceStyleIds.add(ss.getOid());
            double[] colors;
            if (colour != null) {
              colors = new double[] {colour.getRed(), colour.getGreen(), colour.getBlue()};
            } else {
              colors = new double[] {0, 0, 0};
            }
            materialColorMap.put("" + ss.getOid(), colors);
          }
        }
      }
    }
  }

  /**
   * Calculate the length unit conversion used for geometry vertices (meter, millimeter, etc ...).
   */
  private void calculateLengthUnitConversion() {
    SIPrefix lengthUnitPrefix = getLengthUnitPrefix(model);
    if (lengthUnitPrefix == null) {
      lengthUnitConversion = 1;
    } else {
      lengthUnitConversion = Math.pow(10.0, lengthUnitPrefix.getValue());
    }
  }

  /**
   * Writes the {@Link IfcObject} hierarchies as tree structure, where {@link IfcProject}s
   * are the root entity for each hierarchy.
   *
   * @param jsonWriter
   * @throws RenderEngineException
   * @throws SerializerException
   * @throws IOException
   */
  private void writeIfcGeometryTree(JsonWriter jsonWriter) throws RenderEngineException,
      SerializerException, IOException {
    jsonWriter.beginObject();
    jsonWriter.name("data").beginArray();
    for (IfcProject ifcProject : model.getAllWithSubTypes(IfcProject.class)) {
      writeIfcProject(jsonWriter, ifcProject);
    }
    jsonWriter.endArray();
    jsonWriter.endObject();
  }


  private void writeIfcProject(JsonWriter jsonWriter, IfcProject ifcProject)
      throws SerializerException, RenderEngineException, IOException {
    jsonWriter.beginObject();

    jsonWriter.name("id").value(ifcProject.getOid());
    jsonWriter.name("name").value(ifcProject.isSetName() ? ifcProject.getName() : "unknown");
    jsonWriter.name("type").value("IfcProject");

    handleDefineBy(ifcProject);

    EList<IfcRelDecomposes> relList = ifcProject.getIsDecomposedBy();
    if (relList != null && !relList.isEmpty()) {
      jsonWriter.name("decomposedBy").beginArray();
      for (IfcRelDecomposes rel : relList) {
        EList<IfcObjectDefinition> relatedObjects = rel.getRelatedObjects();
        for (IfcObjectDefinition relatedObject : relatedObjects) {
          if (relatedObject instanceof IfcSite) {
            writeIfcSite(jsonWriter, (IfcSite) relatedObject);
          }
        }
      }
      jsonWriter.endArray();
    }
    jsonWriter.endObject();
  }

  private Map<String, Double> handleDefineBy(IfcObject ifcObject) throws IOException {
    Map<String, Double> propertyMap = Maps.newHashMap();
    double storeyArea = 0.0;
    double storeyHeight = 0.0;

    EList<IfcRelDefines> ifcRelDefineses = ifcObject.getIsDefinedBy();

    if (ifcRelDefineses != null && !ifcRelDefineses.isEmpty()) {
      for (IfcRelDefines ifcRelDefines : ifcRelDefineses) {
        IfcRelDefinesByProperties definesByProperties = (IfcRelDefinesByProperties) ifcRelDefines;
        IfcPropertySetDefinition ifcPropertySetDefinition =
            definesByProperties.getRelatingPropertyDefinition();

        if (ifcPropertySetDefinition instanceof IfcPropertySet) {
          EList<IfcProperty> ifcProperties =
              ((IfcPropertySet) ifcPropertySetDefinition).getHasProperties();

          for (IfcProperty property : ifcProperties) {
            IfcPropertySingleValue singleValue = (IfcPropertySingleValue) property;
            String propertyName = singleValue.getName().toLowerCase();
            IfcValue nominalValue = singleValue.getNominalValue();
            if (nominalValue instanceof IfcAreaMeasure) {
              IfcAreaMeasure area = (IfcAreaMeasure) nominalValue;
              storeyArea += area.getWrappedValue() * areaUnitConversion;
            } else if (nominalValue instanceof IfcLengthMeasure) {
              IfcLengthMeasure length = (IfcLengthMeasure) nominalValue;
              if (propertyName.contains("height") || propertyName.contains("elevation")) {
                double wrappedValue = length.getWrappedValue() * lengthUnitConversion;
                storeyHeight = storeyHeight > wrappedValue ? storeyHeight : wrappedValue;
              }
            }
          }
        }
      }
    }
    propertyMap.put(AREA, storeyArea);
    propertyMap.put(HEIGHT, storeyHeight);

    return propertyMap;
  }

  private void writeIfcSite(JsonWriter jsonWriter, IfcSite ifcSite) throws SerializerException,
      RenderEngineException, IOException {
    jsonWriter.beginObject();
    writeGeometricObject(jsonWriter, ifcSite, "IfcSite");
    EList<Integer> longitude = ifcSite.getRefLongitude();
    EList<Integer> latitude = ifcSite.getRefLatitude();

    jsonWriter.name("longitude").value(longitude.toString());
    jsonWriter.name("latitude").value(latitude.toString());
    jsonWriter.name("lengthUnitConversion").value(lengthUnitConversion);

    EList<IfcRelDecomposes> relList = ifcSite.getIsDecomposedBy();
    if (relList != null && !relList.isEmpty()) {
      jsonWriter.name("decomposedBy").beginArray();
      for (IfcRelDecomposes rel : relList) {
        EList<IfcObjectDefinition> relatedObjects = rel.getRelatedObjects();
        for (IfcObjectDefinition relatedObject : relatedObjects) {
          if (relatedObject instanceof IfcBuilding) {
            writeIfcBuilding(jsonWriter, (IfcBuilding) relatedObject);
          }
        }
      }
      jsonWriter.endArray();
    }

    jsonWriter.endObject();
  }

  private void writeIfcBuilding(JsonWriter jsonWriter, IfcBuilding ifcBuilding)
      throws SerializerException, RenderEngineException, IOException {
    Map<String, Double> grossPropertyMap = Maps.newHashMap();
    grossPropertyMap.put(AREA, 0.0);
    grossPropertyMap.put(HEIGHT, 0.0);

    jsonWriter.beginObject();
    writeGeometricObject(jsonWriter, ifcBuilding, "IfcBuilding");

    EList<IfcRelDecomposes> relList = ifcBuilding.getIsDecomposedBy();
    if (relList != null && !relList.isEmpty()) {
      jsonWriter.name("decomposedBy").beginArray();
      for (IfcRelDecomposes rel : relList) {
        EList<IfcObjectDefinition> relatedObjects = rel.getRelatedObjects();
        for (IfcObjectDefinition relatedObject : relatedObjects) {
          if (relatedObject instanceof IfcBuildingStorey) {
            Map<String, Double> propertyMap =
                writeIfcBuildingStorey(jsonWriter, (IfcBuildingStorey) relatedObject);
            if (propertyMap.isEmpty() == false) {
              Double area = propertyMap.get(AREA);
              Double height = propertyMap.get(HEIGHT);

              grossPropertyMap.put(AREA, grossPropertyMap.get(AREA) + area);
              grossPropertyMap.put(HEIGHT, grossPropertyMap.get(HEIGHT) + height);
            }
          }
        }
      }
      jsonWriter.endArray();
    }

    jsonWriter.name(AREA).value(grossPropertyMap.get(AREA));
    jsonWriter.name(HEIGHT).value(grossPropertyMap.get(HEIGHT));

    jsonWriter.endObject();
  }


  private Map<String, Double> writeIfcBuildingStorey(JsonWriter jsonWriter,
      IfcBuildingStorey ifcBuildingStorey) throws SerializerException, RenderEngineException,
      IOException {
    Map<String, Double> grossPropertyMap = Maps.newHashMap();
    grossPropertyMap.put(AREA, 0.0);
    grossPropertyMap.put(HEIGHT, 0.0);

    jsonWriter.beginObject();
    writeGeometricObject(jsonWriter, ifcBuildingStorey, "IfcBuildingStorey");
    EList<IfcRelDecomposes> relList = ifcBuildingStorey.getIsDecomposedBy();
    if (relList != null && !relList.isEmpty()) {
      jsonWriter.name("decomposedBy").beginArray();
      for (IfcRelDecomposes rel : relList) {
        EList<IfcObjectDefinition> relatedObjects = rel.getRelatedObjects();
        for (IfcObjectDefinition relatedObject : relatedObjects) {
          if (relatedObject instanceof IfcProduct) {
            jsonWriter.beginObject();
            writeGeometricObject(jsonWriter, (IfcProduct) relatedObject, null);
            jsonWriter.endObject();
          }
        }
      }
      jsonWriter.endArray();

      for (IfcRelDecomposes rel : relList) {
        EList<IfcObjectDefinition> relatedObjects = rel.getRelatedObjects();
        for (IfcObjectDefinition relatedObject : relatedObjects) {
          if (relatedObject instanceof IfcProduct) {
            Map<String, Double> propertyMap = handleDefineBy((IfcObject) relatedObject);
            if (propertyMap.isEmpty() == false) {
              Double area = propertyMap.get(AREA);
              Double height = propertyMap.get(HEIGHT);

              grossPropertyMap.put(AREA, grossPropertyMap.get(AREA) + area);
              if (height > grossPropertyMap.get(HEIGHT)) {
                grossPropertyMap.put(HEIGHT, height);
              }
            }
          }
        }
      }
    }

    jsonWriter.name(AREA).value(grossPropertyMap.get(AREA));
    jsonWriter.name(HEIGHT).value(grossPropertyMap.get(HEIGHT));

    jsonWriter.endObject();

    return grossPropertyMap;
  }

  private void writeGeometricObject(JsonWriter writer, IfcProduct ifcProduct, String ifcType)
      throws RenderEngineException, SerializerException, IOException {
    String material = getMaterial(ifcProduct);
    log.info(ifcProduct.getClass().getSimpleName() + " " + material);
    writeGeometryFromInstancesGeometryObject(writer, ifcProduct, material, ifcType);
  }

  /**
   * @param ifcProduct
   * @return The name of the material for the {@link IfcProduct}.
   */
  private String getMaterial(IfcProduct ifcProduct) {
    IfcProductRepresentation representation = ifcProduct.getRepresentation();
    if (representation != null) {
      EList<IfcRepresentation> representations = representation.getRepresentations();
      for (IfcRepresentation rep : representations) {
        EList<IfcRepresentationItem> items = rep.getItems();
        for (IfcRepresentationItem item : items) {
          if (item instanceof IfcStyledItem) {
            String material = getSurfaceStyleRenderingOid((IfcStyledItem) item);
            if (!Strings.isNullOrEmpty(material)) {
              return material;
            }
          } else {
            EList<IfcStyledItem> styledByItem = item.getStyledByItem();
            for (IfcStyledItem sItem : styledByItem) {
              String material = getSurfaceStyleRenderingOid(sItem);
              if (!Strings.isNullOrEmpty(material)) {
                return material;
              }
            }
          }
        }
      }
    }

    return UNKNOWN_STYLE;
  }

  /**
   * Updates the material with {@link IfcSurfaceStyle} name of the {@link IfcStyledItem}, if exist.
   *
   * @param sItem
   * @return The material name.
   */
  private String getSurfaceStyleRenderingOid(IfcStyledItem sItem) {
    EList<IfcPresentationStyleAssignment> styles = sItem.getStyles();
    for (IfcPresentationStyleAssignment sa : styles) {
      EList<IfcPresentationStyleSelect> styles2 = sa.getStyles();
      for (IfcPresentationStyleSelect pss : styles2) {
        if (pss instanceof IfcSurfaceStyle) {
          IfcSurfaceStyle ss = (IfcSurfaceStyle) pss;
          for (IfcSurfaceStyleElementSelect style : ss.getStyles()) {
            if (style instanceof IfcSurfaceStyleRendering) {
              return "" + ss.getOid();
            }
          }

        }
      }
    }
    return null;
  }

  private void writeGeometryFromInstancesGeometryObject(JsonWriter jsonWriter,
      IfcProduct ifcObject, String material, String ifcType) throws IOException,
      RenderEngineException, SerializerException {
    GeometryInfo geometryInfo = ifcObject.getGeometry();
    jsonWriter.name("id").value(ifcObject.getOid());
    jsonWriter.name("name").value(ifcObject.isSetName() ? ifcObject.getName() : "unknown");
    jsonWriter.name("type").value(
        (ifcType != null) ? ifcType : stripClassName(ifcObject.getClass()));

    log.info("ifc type: " + stripClassName(ifcObject.getClass()));

    if (geometryInfo != null && geometryInfo.getData() != null) {
      GeometryData geometryData = geometryInfo.getData();
      ByteBuffer indicesBuffer = ByteBuffer.wrap(geometryData.getIndices());
      indicesBuffer.order(ByteOrder.LITTLE_ENDIAN);
      ByteBuffer verticesBuffer = ByteBuffer.wrap(geometryData.getVertices());
      verticesBuffer.order(ByteOrder.LITTLE_ENDIAN);
      ByteBuffer normalsBuffer = ByteBuffer.wrap(geometryData.getNormals());
      normalsBuffer.order(ByteOrder.LITTLE_ENDIAN);

      for (GeometryData data : geometryDatas) {
        if (isSameBytes(geometryData.getVertices(), data.getVertices())
            && isSameBytes(geometryData.getIndices(), data.getIndices())) {
          sameGeometry++;
          log.info("found same geometry data count: " + sameGeometry);
          break;
        }
        geometryDatas.add(geometryData);
      }

      int totalNrVertexValues = verticesBuffer.capacity() / 4;
      int maxVertexValues = 49167; // Must be divisible by 9!

      jsonWriter.name("geometry").beginObject();

      double[] colorData;
      if (materialColorMap.containsKey(material)) {
        colorData = materialColorMap.get(material);
      } else {
        colorData = new double[] {1.0, 1.0, 1.0};
      }

      jsonWriter.name("material").value(material);
      jsonWriter.name("color").beginArray();
      writeDouble(jsonWriter, colorData[0]);
      writeDouble(jsonWriter, colorData[1]);
      writeDouble(jsonWriter, colorData[2]);
      writeDouble(jsonWriter, 1.0);
      jsonWriter.endArray();

      if (totalNrVertexValues > maxVertexValues) {} else {
        jsonWriter.name("primitive").value("triangles");
        jsonWriter.name("positions").beginArray();
        for (int i = 0; i < totalNrVertexValues; i++) {
          writeDouble(jsonWriter, verticesBuffer.getFloat());
        }
        jsonWriter.endArray();
        jsonWriter.name("normals").beginArray();
        for (int i = 0; i < totalNrVertexValues; i++) {
          writeDouble(jsonWriter, normalsBuffer.getFloat());
        }
        jsonWriter.endArray();
        jsonWriter.name("triangles").beginArray();
        for (int i = 0; i < indicesBuffer.capacity() / 4; i += 3) {
          writeInteger(jsonWriter, indicesBuffer.getInt());
          writeInteger(jsonWriter, indicesBuffer.getInt());
          writeInteger(jsonWriter, indicesBuffer.getInt());
        }
        jsonWriter.endArray();

        byte[] geometryTransformation = geometryInfo.getTransformation();
        if (geometryTransformation != null) {
          ByteBuffer transformation = ByteBuffer.wrap(geometryTransformation);
          transformation.order(ByteOrder.LITTLE_ENDIAN);
          FloatBuffer floatBuffer = transformation.asFloatBuffer();
          float[] matrix = new float[16];
          for (int i = 0; i < matrix.length; i++) {
            matrix[i] = floatBuffer.get();
          }
          matrix = Matrix.changeOrientation(matrix);
          jsonWriter.name("matrix").beginArray();
          for (int i = 0; i < matrix.length; i++) {
            writeDouble(jsonWriter, matrix[i]);
          }
          jsonWriter.endArray();
        }
      }

      jsonWriter.endObject();
    }
    if (ifcObject instanceof IfcSpatialStructureElement) {
      IfcSpatialStructureElement spatialStructureElement = (IfcSpatialStructureElement) ifcObject;
      EList<IfcRelContainedInSpatialStructure> relList =
          spatialStructureElement.getContainsElements();
      if (relList != null && !relList.isEmpty()) {
        jsonWriter.name("contains");
        jsonWriter.beginArray();
        for (IfcRelContainedInSpatialStructure rel : relList) {
          for (IfcProduct ifcProduct : rel.getRelatedElements()) {
            jsonWriter.beginObject();
            writeGeometricObject(jsonWriter, ifcProduct, null);
            jsonWriter.endObject();
          }
        }
        jsonWriter.endArray();
      }
    }
  }

  private boolean isSameBytes(byte[] bytes1, byte[] bytes2) {
    if (bytes1.length != bytes2.length) {
      return false;
    }
    for (int i = 0; i < bytes1.length; i++) {
      if (bytes1[i] != bytes2[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Writes a double value to json string. If value is NaN, writes 0 instead.
   *
   * @param jsonWriter
   * @param value
   * @throws java.io.IOException
   */
  private void writeDouble(JsonWriter jsonWriter, double value) throws IOException {
    if (Double.isNaN(value)) {
      value = 0;
    }
    jsonWriter.value(Double.valueOf(value));
  }

  private void writeInteger(JsonWriter jsonWriter, Integer value) throws IOException {
    jsonWriter.value(value.longValue());
  }

  @SuppressWarnings("unused")
  private void reorder(ByteBuffer buffer, int nrFloats) {
    buffer.position(0);
    for (int i = 0; i < nrFloats; i += 9) {
      float x1 = buffer.getFloat();
      float y1 = buffer.getFloat();
      float z1 = buffer.getFloat();
      float x2 = buffer.getFloat();
      float y2 = buffer.getFloat();
      float z2 = buffer.getFloat();
      float x3 = buffer.getFloat();
      float y3 = buffer.getFloat();
      float z3 = buffer.getFloat();
      buffer.putFloat((i + 3) * 4, x3);
      buffer.putFloat((i + 4) * 4, y3);
      buffer.putFloat((i + 5) * 4, z3);
      buffer.putFloat((i + 6) * 4, x2);
      buffer.putFloat((i + 7) * 4, y2);
      buffer.putFloat((i + 8) * 4, z2);
    }
    buffer.position(0);
  }

  /**
   * @param classObject
   * @return The IFC object class name without the 'Impl' suffix.
   */
  private static String stripClassName(Class<?> classObject) {
    String name = classObject.getSimpleName();
    int implIndex = name.lastIndexOf("Impl");
    return name.substring(0, implIndex < 0 ? name.length() : implIndex);
  }

  private String fitNameForQualifiedName(String name) {
    if (name == null) {
      return "Null";
    }
    StringBuilder builder = new StringBuilder(name);
    int indexOfChar = builder.indexOf(" ");
    while (indexOfChar >= 0) {
      builder.deleteCharAt(indexOfChar);
      indexOfChar = builder.indexOf(" ");
    }
    indexOfChar = builder.indexOf(",");
    while (indexOfChar >= 0) {
      builder.setCharAt(indexOfChar, '_');
      indexOfChar = builder.indexOf(",");
    }
    indexOfChar = builder.indexOf("/");
    while (indexOfChar >= 0) {
      builder.setCharAt(indexOfChar, '_');
      indexOfChar = builder.indexOf("/");
    }
    indexOfChar = builder.indexOf("*");
    while (indexOfChar >= 0) {
      builder.setCharAt(indexOfChar, '_');
      indexOfChar = builder.indexOf("/");
    }
    return builder.toString();
  }

  private static SIPrefix getLengthUnitPrefix(IfcModelInterface model) {
    SIPrefix lengthUnitPrefix = null;
    boolean prefixFound = false;
    Map<Long, IdEObject> objects = model.getObjects();
    for (IdEObject object : objects.values()) {
      if (object instanceof IfcProject) {
        IfcUnitAssignment unitsInContext = ((IfcProject) object).getUnitsInContext();
        if (unitsInContext != null) {
          EList<IfcUnit> units = unitsInContext.getUnits();
          for (IfcUnit unit : units) {
            if (unit instanceof IfcSIUnit) {
              IfcSIUnit ifcSIUnit = (IfcSIUnit) unit;
              IfcUnitEnum unitType = ifcSIUnit.getUnitType();
              if (unitType == IfcUnitEnum.LENGTHUNIT) {
                prefixFound = true;
                switch (ifcSIUnit.getPrefix()) {
                  case EXA:
                    lengthUnitPrefix = SIPrefix.EXAMETER;
                    break;
                  case PETA:
                    lengthUnitPrefix = SIPrefix.PETAMETER;
                    break;
                  case TERA:
                    lengthUnitPrefix = SIPrefix.TERAMETER;
                    break;
                  case GIGA:
                    lengthUnitPrefix = SIPrefix.GIGAMETER;
                    break;
                  case MEGA:
                    lengthUnitPrefix = SIPrefix.MEGAMETER;
                    break;
                  case KILO:
                    lengthUnitPrefix = SIPrefix.KILOMETER;
                    break;
                  case HECTO:
                    lengthUnitPrefix = SIPrefix.HECTOMETER;
                    break;
                  case DECA:
                    lengthUnitPrefix = SIPrefix.DECAMETER;
                    break;
                  case DECI:
                    lengthUnitPrefix = SIPrefix.DECIMETER;
                    break;
                  case CENTI:
                    lengthUnitPrefix = SIPrefix.CENTIMETER;
                    break;
                  case MILLI:
                    lengthUnitPrefix = SIPrefix.MILLIMETER;
                    break;
                  case MICRO:
                    lengthUnitPrefix = SIPrefix.MICROMETER;
                    break;
                  case NANO:
                    lengthUnitPrefix = SIPrefix.NANOMETER;
                    break;
                  case PICO:
                    lengthUnitPrefix = SIPrefix.PICOMETER;
                    break;
                  case FEMTO:
                    lengthUnitPrefix = SIPrefix.FEMTOMETER;
                    break;
                  case ATTO:
                    lengthUnitPrefix = SIPrefix.ATTOMETER;
                    break;
                  case NULL:
                    lengthUnitPrefix = SIPrefix.METER;
                    break;
                }
                break;
              }
            }
          }
        }
      }
      if (prefixFound) break;
    }
    return lengthUnitPrefix;
  }
}
