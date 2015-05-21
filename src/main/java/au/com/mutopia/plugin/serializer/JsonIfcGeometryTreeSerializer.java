package au.com.mutopia.plugin.serializer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bimserver.geometry.Matrix;
import org.bimserver.models.ifc2x3tc1.GeometryData;
import org.bimserver.models.ifc2x3tc1.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcBooleanClippingResult;
import org.bimserver.models.ifc2x3tc1.IfcBooleanOperand;
import org.bimserver.models.ifc2x3tc1.IfcBooleanResult;
import org.bimserver.models.ifc2x3tc1.IfcColourRgb;
import org.bimserver.models.ifc2x3tc1.IfcCsgPrimitive3D;
import org.bimserver.models.ifc2x3tc1.IfcElementQuantity;
import org.bimserver.models.ifc2x3tc1.IfcGroup;
import org.bimserver.models.ifc2x3tc1.IfcHalfSpaceSolid;
import org.bimserver.models.ifc2x3tc1.IfcMappedItem;
import org.bimserver.models.ifc2x3tc1.IfcMaterial;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayer;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayerSet;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayerSetUsage;
import org.bimserver.models.ifc2x3tc1.IfcMaterialSelect;
import org.bimserver.models.ifc2x3tc1.IfcObject;
import org.bimserver.models.ifc2x3tc1.IfcObjectDefinition;
import org.bimserver.models.ifc2x3tc1.IfcPhysicalQuantity;
import org.bimserver.models.ifc2x3tc1.IfcPresentationStyleAssignment;
import org.bimserver.models.ifc2x3tc1.IfcPresentationStyleSelect;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcProductRepresentation;
import org.bimserver.models.ifc2x3tc1.IfcProject;
import org.bimserver.models.ifc2x3tc1.IfcProperty;
import org.bimserver.models.ifc2x3tc1.IfcPropertySet;
import org.bimserver.models.ifc2x3tc1.IfcPropertySetDefinition;
import org.bimserver.models.ifc2x3tc1.IfcPropertySingleValue;
import org.bimserver.models.ifc2x3tc1.IfcRelAssigns;
import org.bimserver.models.ifc2x3tc1.IfcRelAssignsToGroup;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociates;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociatesMaterial;
import org.bimserver.models.ifc2x3tc1.IfcRelContainedInSpatialStructure;
import org.bimserver.models.ifc2x3tc1.IfcRelDecomposes;
import org.bimserver.models.ifc2x3tc1.IfcRelDefines;
import org.bimserver.models.ifc2x3tc1.IfcRelDefinesByProperties;
import org.bimserver.models.ifc2x3tc1.IfcRelDefinesByType;
import org.bimserver.models.ifc2x3tc1.IfcRepresentation;
import org.bimserver.models.ifc2x3tc1.IfcRepresentationItem;
import org.bimserver.models.ifc2x3tc1.IfcRoot;
import org.bimserver.models.ifc2x3tc1.IfcSite;
import org.bimserver.models.ifc2x3tc1.IfcSolidModel;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
import org.bimserver.models.ifc2x3tc1.IfcSpatialStructureElement;
import org.bimserver.models.ifc2x3tc1.IfcStyledItem;
import org.bimserver.models.ifc2x3tc1.IfcSurfaceStyle;
import org.bimserver.models.ifc2x3tc1.IfcSurfaceStyleElementSelect;
import org.bimserver.models.ifc2x3tc1.IfcSurfaceStyleRendering;
import org.bimserver.models.ifc2x3tc1.IfcZone;
import org.bimserver.models.store.SIPrefix;
import org.bimserver.plugins.renderengine.RenderEngineException;
import org.bimserver.plugins.serializers.AbstractGeometrySerializer;
import org.bimserver.plugins.serializers.SerializerException;
import org.eclipse.emf.common.util.EList;

import au.com.mutopia.plugin.util.IfcUtil;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.gson.stream.JsonWriter;

/**
 * Serializer for BimServer, to extract Ifc object hierarchy, color and parameters.
 */
public class JsonIfcGeometryTreeSerializer extends AbstractGeometrySerializer {
  private static final Logger log = Logger.getLogger(JsonIfcGeometryTreeSerializer.class.getName());

  private final IfcUtil ifcUtil = new IfcUtil();

  public static final String AREA = "area";
  public static final String HEIGHT = "height";
  public static final String UNKNOWN_STYLE = "UNKNOWN";

  private static final String PARAMETER = "parameters";

  private static final String LONG_NAME_PROPERTY = "long_name";
  private static final String ZONE_PROPERTY = "zone";
  private static final String SPACE_CODE_PROPERTY = "space_code";
  private static final String MATERIAL_NAME_PROPERTY = "material_name";
  private static final String MATERIAL_THICKNESS_PROPERTY = "material_thickness";

  private List<Long> surfaceStyleIds = new ArrayList<>();
  private HashMap<String, double[]> materialColorMap = new HashMap<>();
  private HashMap<IfcObject, IfcMaterialSelect> objectMaterialMap = new HashMap<>();

  private List<GeometryData> geometryDatas = new ArrayList<>();
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
        mapObjectMaterials();
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
   * Calculates the length unit conversion used for geometry vertices (meter, millimeter, etc ...).
   */
  private void calculateLengthUnitConversion() {
    SIPrefix lengthUnitPrefix = ifcUtil.getLengthUnitPrefix(model);
    if (lengthUnitPrefix == null) {
      lengthUnitConversion = 1;
    } else {
      lengthUnitConversion = Math.pow(10.0, lengthUnitPrefix.getValue());
    }
  }

  /**
   * Maps {@link IfcObject}s to their corresponding {@link IfcMaterialSelect}s, which contains data
   * regarding the type and/or thickness of the material layers.
   */
  private void mapObjectMaterials() {
    for (IfcRelAssociatesMaterial associatesMaterial :
        model.getAllWithSubTypes(IfcRelAssociatesMaterial.class)) {
      for (IfcRoot root : associatesMaterial.getRelatedObjects()) {
        if (!(root instanceof IfcObject)) continue;
        IfcObject object = (IfcObject) root;
        objectMaterialMap.put(object, associatesMaterial.getRelatingMaterial());
      }
    }
  }

  /**
   * Writes the {@Link IfcObject} hierarchies as tree structure, where {@link IfcProject}s
   * are the root entity for each hierarchy.
   *
   * @param writer
   * @throws RenderEngineException
   * @throws SerializerException
   * @throws IOException
   */
  private void writeIfcGeometryTree(JsonWriter writer) throws RenderEngineException,
      SerializerException, IOException {
    writer.beginObject();
    writer.name("data").beginArray();
    for (IfcProject ifcProject : model.getAllWithSubTypes(IfcProject.class)) {
      writeIfcTreeObject(writer, ifcProject);
    }
    writer.endArray();
    writer.endObject();
  }

  /**
   * Writes the {@Link IfcObject} within the tree hierarchy. Writes the longitude and latitude if
   * the object is {@link IfcSite}.
   *
   * @param writer
   * @param object The {@Link IfcObject} within the tree hierarchy.
   * @throws IOException
   */
  private void writeIfcTreeObject(JsonWriter writer, IfcObject object) throws IOException {
    writer.beginObject();
    writer.name("id").value(object.getGlobalId());
    String name = "unknown";
    if (object.isSetName()) {
      name = object.getName();
    }
    String type = object.getObjectType();
    if (Strings.isNullOrEmpty(type)) {
      type = ifcUtil.stripClassName(object.getClass());
    }
    if (object instanceof IfcSpatialStructureElement) {
      IfcSpatialStructureElement spatialStructureElement = (IfcSpatialStructureElement) object;
      if (spatialStructureElement instanceof IfcSite) {
        type = ifcUtil.stripClassName(IfcSite.class);

        IfcSite site = (IfcSite) spatialStructureElement;
        EList<Integer> longitude = site.getRefLongitude();
        EList<Integer> latitude = site.getRefLatitude();

        writer.name("longitude").value(longitude.toString());
        writer.name("latitude").value(latitude.toString());
        writer.name("lengthUnitConversion").value(lengthUnitConversion);
      }
      writeIfcTreeContainsElements(writer, spatialStructureElement);
    }
    writer.name("name").value(name);
    writer.name("type").value(type);
    writeIfcTreeDecomposedBy(writer, object);
    writeParameters(writer, object);

    if (object instanceof IfcProduct) {
      writeMaterialAndGeometry(writer, (IfcProduct) object);
    }
    writer.endObject();
  }

  /**
   * Writes the list of {@Link IfcObject}s that decomposes another parent {@Link IfcObject}.
   *
   * @param jsonWriter
   * @param objectDefinition The parent {@Link IfcObject}.
   * @throws IOException
   */
  private void writeIfcTreeDecomposedBy(JsonWriter jsonWriter, IfcObjectDefinition objectDefinition)
      throws IOException {
    EList<IfcRelDecomposes> relList = objectDefinition.getIsDecomposedBy();
    if (relList != null && !relList.isEmpty()) {
      jsonWriter.name("decomposedBy").beginArray();
      for (IfcRelDecomposes rel : relList) {
        EList<IfcObjectDefinition> relatedObjects = rel.getRelatedObjects();
        for (IfcObjectDefinition relatedObject : relatedObjects) {
          if (relatedObject instanceof IfcObject) {
            writeIfcTreeObject(jsonWriter, (IfcObject) relatedObject);
          }
        }
      }
      jsonWriter.endArray();
    }
  }

  /**
   * Writes the list of Parameters that defines the {@Link IfcObject}.
   *
   * @param writer
   * @param object The {@Link IfcObject}.
   * @throws IOException
   */
  private void writeParameters(JsonWriter writer, IfcObject object) throws IOException {
    Map<String, String> parameters = new HashMap<>();
    parameters.putAll(getPropertiesFromDefinedBy(object.getIsDefinedBy()));
    parameters.putAll(getParametersFromAssignments(object.getHasAssignments()));
    parameters.putAll(getParametersFromAssociates(object.getHasAssociations()));
    parameters.putAll(getParametersFromMaterialSelect(object));

    if (object instanceof IfcSpatialStructureElement) {
      IfcSpatialStructureElement spatialStructureElement = (IfcSpatialStructureElement) object;
      if (spatialStructureElement.isSetLongName()) {
        parameters.put(LONG_NAME_PROPERTY, spatialStructureElement.getLongName());
      }
    }
    if (parameters != null && !parameters.isEmpty()) {
      writer.name(PARAMETER).beginObject();
      for (String parameterName : parameters.keySet()) {
        writer.name(parameterName).value(parameters.get(parameterName));
      }
      writer.endObject();
    }
  }

  /**
   * Collects the {key, value} map for properties extracted from the list of {@link IfcRelDefines}s.
   *
   * @param isDefinedBy
   * @return The {key, value} map properties from a list of {@link IfcRelDefines} of an
   * {@link IfcObject};
   */
  private Map<String, String> getPropertiesFromDefinedBy(EList<IfcRelDefines> isDefinedBy) {
    Map<String, String> parameters = new HashMap<>();
    if (isDefinedBy == null || isDefinedBy.isEmpty()) {
      return parameters;
    }
    for (IfcRelDefines ifcRelDefines : isDefinedBy) {
      if (ifcRelDefines instanceof IfcRelDefinesByProperties) {
        IfcRelDefinesByProperties relDefinesByProperties =
            (IfcRelDefinesByProperties) ifcRelDefines;
        IfcPropertySetDefinition relatingPropertyDefinition =
            relDefinesByProperties.getRelatingPropertyDefinition();
        if (relatingPropertyDefinition instanceof IfcPropertySet) {
          IfcPropertySet propertySet = (IfcPropertySet) relatingPropertyDefinition;
          for (IfcProperty ifcProperty : propertySet.getHasProperties()) {
            if (ifcProperty instanceof IfcPropertySingleValue) {
              IfcPropertySingleValue ifcPropertySingleValue = (IfcPropertySingleValue) ifcProperty;
              String value =
                  ifcUtil.getStringValueFromIfcValue(ifcPropertySingleValue.getNominalValue());
              if (!Strings.isNullOrEmpty(value)) {
                parameters.put(ifcPropertySingleValue.getName(), value);
              }
            } else {
              log.info("Unknown IfcProperty value : " + ifcProperty.getName());
              continue;
            }
          }
        } else if (relatingPropertyDefinition instanceof IfcElementQuantity) {
          IfcElementQuantity ifcElementQuantity = (IfcElementQuantity) relatingPropertyDefinition;
          for (IfcPhysicalQuantity ifcPhysicalQuantity : ifcElementQuantity.getQuantities()) {
            String value = ifcUtil.getStringValueFromIfcPhysicalQuantity(ifcPhysicalQuantity);
            if (!Strings.isNullOrEmpty(value)) {
              parameters.put(ifcPhysicalQuantity.getName(), value);
            }
          }
        } else {
          log.info("Unknown IfcPropertySetDefinition : " + relatingPropertyDefinition);
          continue;
        }
      } else if (ifcRelDefines instanceof IfcRelDefinesByType) {
        IfcRelDefinesByType relDefinesByType = (IfcRelDefinesByType) ifcRelDefines;
        parameters.put(SPACE_CODE_PROPERTY, relDefinesByType.getRelatingType().getName());
      }
    }
    return parameters;
  }

  /**
   * Collects the {key, value} map for properties extracted from the list of {@link IfcRelAssigns}s.
   *
   * @param assignments
   * @return The {key, value} map properties from a list of {@link IfcRelAssigns} of an
   * {@link IfcObject};
   */
  private Map<String, String> getParametersFromAssignments(EList<IfcRelAssigns> assignments) {
    Map<String, String> parameters = new HashMap<>();
    if (assignments == null || assignments.isEmpty()) {
      return parameters;
    }
    for (IfcRelAssigns assignment : assignments) {
      if (assignment instanceof IfcRelAssignsToGroup) {
        IfcRelAssignsToGroup relAssignsToGroup = (IfcRelAssignsToGroup) assignment;
        IfcGroup relatingGroup = relAssignsToGroup.getRelatingGroup();
        if (relatingGroup instanceof IfcZone) {
          parameters.put(ZONE_PROPERTY , relatingGroup.getName());
        }
      }
    }
    return parameters;
  }

  /**
   * Collects the {key, value} map for properties extracted from the list of
   * {@link IfcRelAssociates}s.
   *
   * @param associations
   * @return The {key, value} map properties from a list of {@link IfcRelAssociates} of an
   * {@link IfcObject};
   */
  private Map<String, String> getParametersFromAssociates(EList<IfcRelAssociates> associations) {
    Map<String, String> parameters = new HashMap<>();
    if (associations == null || associations.isEmpty()) {
      return parameters;
    }
    for (IfcRelAssociates association : associations) {
      if (association instanceof IfcRelAssociatesMaterial) {
        IfcRelAssociatesMaterial associatesMaterial = (IfcRelAssociatesMaterial) association;
        parameters.putAll(getParameterFromMaterial(associatesMaterial.getRelatingMaterial()));
      }
    }
    return parameters;
  }

  /**
   * Collects the {key, value} map for properties regarding the {@link IfcObject}'s material.
   *
   * @param object
   * @return The {key, value} map value properties describing the {@link IfcObject}'s material.
   */
  private Map<String, String> getParametersFromMaterialSelect(IfcObject object) {
    Map<String, String> parameters = new HashMap<>();
    if (!objectMaterialMap.containsKey(object)) {
      return parameters;
    }
    return getParameterFromMaterial(objectMaterialMap.get(object));
  }

  /**
   * Collects the {key, value} map for properties from {@link IfcMaterialSelect} which describe the
   * material properties.
   *
   * @param materialSelect
   * @return The {key, value} map value properties describing the {@link IfcMaterialSelect}.
   */
  private Map<String, String> getParameterFromMaterial(IfcMaterialSelect materialSelect) {
    Map<String, String> parameters = new HashMap<>();
    if (materialSelect instanceof IfcMaterial) {
      parameters.put(MATERIAL_NAME_PROPERTY, ((IfcMaterial) materialSelect).getName());
    } else if (materialSelect instanceof IfcMaterialLayerSetUsage) {
      IfcMaterialLayerSetUsage materialLayerSetUsage = (IfcMaterialLayerSetUsage) materialSelect;
      IfcMaterialLayerSet layerSet = materialLayerSetUsage.getForLayerSet();
      EList<IfcMaterialLayer> materialLayers = layerSet.getMaterialLayers();
      IfcMaterialLayer ifcMaterialLayer = materialLayers.get(0);
      parameters.put(MATERIAL_NAME_PROPERTY, ifcMaterialLayer.getMaterial().getName());
      parameters.put(MATERIAL_THICKNESS_PROPERTY,
          Double.toString(ifcMaterialLayer.getLayerThickness()));
    }
    return parameters;
  }

  /**
   * Writes the list of {@link IfcProduct}s that is contained within the parent
   * {@link IfcSpatialStructureElement}.
   *
   * @param spatialStructureElement The parent {@link IfcSpatialStructureElement}.
   * @throws IOException
   */
  private void writeIfcTreeContainsElements(JsonWriter writer,
      IfcSpatialStructureElement spatialStructureElement) throws IOException {
    EList<IfcRelContainedInSpatialStructure> relList =
        spatialStructureElement.getContainsElements();
    if (relList != null && !relList.isEmpty()) {
      writer.name("contains");
      writer.beginArray();
      for (IfcRelContainedInSpatialStructure rel : relList) {
        for (IfcProduct ifcProduct : rel.getRelatedElements()) {
          writeIfcTreeObject(writer, ifcProduct);
        }
      }
      writer.endArray();
    }
  }

  /**
   * Writes the material and geometry for the {@link IfcProduct}.
   *
   * @param writer
   * @param product The {@link IfcProduct} with material and geometry.
   * @throws IOException
   */
  private void writeMaterialAndGeometry(JsonWriter writer, IfcProduct product) throws IOException {
    GeometryInfo geometryInfo = product.getGeometry();
    if (geometryInfo != null && geometryInfo.getData() != null) {
      GeometryData geometryData = geometryInfo.getData();
      ByteBuffer indicesBuffer = ByteBuffer.wrap(geometryData.getIndices());
      indicesBuffer.order(ByteOrder.LITTLE_ENDIAN);
      ByteBuffer verticesBuffer = ByteBuffer.wrap(geometryData.getVertices());
      verticesBuffer.order(ByteOrder.LITTLE_ENDIAN);
      ByteBuffer normalsBuffer = ByteBuffer.wrap(geometryData.getNormals());
      normalsBuffer.order(ByteOrder.LITTLE_ENDIAN);

      int totalNrVertexValues = verticesBuffer.capacity() / 4;
      int maxVertexValues = 49167; // Must be divisible by 9!

      writer.name("geometry").beginObject();

      double[] colorData = getMaterial(product);
      if (colorData == null) {
        log.info("No material styles found for: " + product.getName());
        if (product instanceof IfcSpace) {
          colorData = new double[] {0.0, 1.0, 0.0, 0.9};
        } else {
          colorData = new double[] {1.0, 1.0, 1.0, 1.0};
        }
      }
      writer.name("color").beginArray();
      writeDouble(writer, colorData[0]);
      writeDouble(writer, colorData[1]);
      writeDouble(writer, colorData[2]);
      writeDouble(writer, colorData[3]);
      writer.endArray();

      if (totalNrVertexValues > maxVertexValues) {} else {
        writer.name("primitive").value("triangles");
        writer.name("positions").beginArray();
        for (int i = 0; i < totalNrVertexValues; i++) {
          writeDouble(writer, verticesBuffer.getFloat());
        }
        writer.endArray();
        writer.name("normals").beginArray();
        for (int i = 0; i < totalNrVertexValues; i++) {
          writeDouble(writer, normalsBuffer.getFloat());
        }
        writer.endArray();
        writer.name("triangles").beginArray();
        for (int i = 0; i < indicesBuffer.capacity() / 4; i += 3) {
          writeInteger(writer, indicesBuffer.getInt());
          writeInteger(writer, indicesBuffer.getInt());
          writeInteger(writer, indicesBuffer.getInt());
        }
        writer.endArray();

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
          writer.name("matrix").beginArray();
          for (int i = 0; i < matrix.length; i++) {
            writeDouble(writer, matrix[i]);
          }
          writer.endArray();
        }
      }

      writer.endObject();
    }
  }

  /**
   * Searches all {@link IfcStyledItem} that is referenced to the {@link IfcProduct} and returns
   * the color {red, green, blue, alpha} for the {@link IfcProduct}.
   *
   * @param ifcProduct
   * @return The float array of the {@link IfcProduct}'s material's color. Null if doesn't exist.
   */
  private double[] getMaterial(IfcProduct ifcProduct) {
    IfcProductRepresentation representation = ifcProduct.getRepresentation();
    if (representation != null) {
      EList<IfcRepresentation> representations = representation.getRepresentations();
      for (IfcRepresentation rep : representations) {
        double[] material = getMaterial(rep);
        if (material != null) {
          return material;
        }
      }
    }
    return null;
  }

  /**
   * Searches all {@link IfcStyledItem} that is referenced to the {@link IfcRepresentation} and
   * returns the color {red, green, blue, alpha} for the {@link IfcRepresentation}.
   *
   * @param ifcRepresentation
   * @return The float array of the {@link IfcProduct}'s material's color. Null if doesn't exist.
   */
  private double[] getMaterial(IfcRepresentation ifcRepresentation) {
    EList<IfcRepresentationItem> items = ifcRepresentation.getItems();
    for (IfcRepresentationItem item : items) {
      if (item instanceof IfcStyledItem) {
        double[] material = getColorAndTransparency((IfcStyledItem) item);
        if (material != null) {
          return material;
        }
      } else {
        if (item instanceof IfcBooleanClippingResult) {
          for (IfcStyledItem sItem :
              getStyledItemsFromBooleanResult((IfcBooleanClippingResult) item)) {
            double[] material = getColorAndTransparency(sItem);
            if (material != null) {
              return material;
            }
          }
        }
        EList<IfcStyledItem> styledByItem = item.getStyledByItem();
        for (IfcStyledItem sItem : styledByItem) {
          double[] material = getColorAndTransparency(sItem);
          if (material != null) {
            return material;
          }
        }
        if (item instanceof IfcMappedItem) {
          IfcMappedItem mappedItem = (IfcMappedItem) item;
          double[] material = getMaterial(mappedItem.getMappingSource().getMappedRepresentation());
          if (material != null) {
            return material;
          }
        }
      }
    }
    return null;
  }

  /**
   * Collects all {@link IfcStyledItem}s referenced by the {@link IfcBooleanResult} and its
   * components.
   *
   * @param booleanResult The {@link IfcBooleanResult} to extract the {@link IfcStyledItem}s from.
   * @return The list of {@link IfcStyledItem}s.
   */
  private List<IfcStyledItem> getStyledItemsFromBooleanResult(IfcBooleanResult booleanResult) {
    List<IfcStyledItem> styledItems = new ArrayList<>();
    styledItems.addAll(getStyledItemsFromBooleanOperand(booleanResult.getFirstOperand()));
    styledItems.addAll(getStyledItemsFromBooleanOperand(booleanResult.getSecondOperand()));
    return styledItems;
  }

  /**
   * Collects all {@link IfcStyledItem}s referenced by the {@link IfcBooleanOperand}.
   *
   * @param operand The {@link IfcBooleanOperand}.
   * @return The {@link IfcStyledItem}s referenced by the {@link IfcBooleanOperand}.
   */
  private List<IfcStyledItem> getStyledItemsFromBooleanOperand(IfcBooleanOperand operand) {
    List<IfcStyledItem> styledItems = new ArrayList<>();
    if (operand instanceof IfcBooleanResult) {
      styledItems.addAll(getStyledItemsFromBooleanResult((IfcBooleanResult) operand));
    } else if (operand instanceof IfcSolidModel) {
      styledItems.addAll(((IfcSolidModel) operand).getStyledByItem());
    } else if (operand instanceof IfcHalfSpaceSolid) {
      styledItems.addAll(((IfcHalfSpaceSolid) operand).getStyledByItem());
    } else if (operand instanceof IfcCsgPrimitive3D) {
      styledItems.addAll(((IfcCsgPrimitive3D) operand).getStyledByItem());
    }
    return styledItems;
  }

  /**
   *
   * @param sItem
   * @return The style color and transparency in array of doubles {red, green, blue, alpha}.
   */
  private double[] getColorAndTransparency(IfcStyledItem sItem) {
    EList<IfcPresentationStyleAssignment> styles = sItem.getStyles();
    for (IfcPresentationStyleAssignment sa : styles) {
      EList<IfcPresentationStyleSelect> styles2 = sa.getStyles();
      for (IfcPresentationStyleSelect pss : styles2) {
        if (pss instanceof IfcSurfaceStyle) {
          IfcSurfaceStyle ss = (IfcSurfaceStyle) pss;
          for (IfcSurfaceStyleElementSelect style : ss.getStyles()) {
            if (style instanceof IfcSurfaceStyleRendering) {
              IfcSurfaceStyleRendering ssr = (IfcSurfaceStyleRendering) style;
              IfcColourRgb colour = ssr.getSurfaceColour();
              double alpha = 1 - ssr.getTransparency();
              if (colour != null) {
                return new double[] {colour.getRed(), colour.getGreen(), colour.getBlue(), alpha};
              }
            } else {
              log.info("Surface style type: " + style.getClass().getSimpleName() + " is not " +
                  "supported yet.");
            }
          }
        }
      }
    }
    return null;
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
}
