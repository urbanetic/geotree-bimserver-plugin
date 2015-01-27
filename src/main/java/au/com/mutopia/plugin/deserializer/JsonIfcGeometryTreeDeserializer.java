package au.com.mutopia.plugin.deserializer;

import au.com.mutopia.plugin.deserializer.guid.GuidCompressor;
import au.com.mutopia.plugin.serializer.JsonIfcGeometryTreeSerializer;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IdEObjectImpl;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.emf.IfcModelInterfaceException;
import org.bimserver.ifc.IfcModel;
import org.bimserver.models.ifc2x3tc1.*;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.deserializers.EmfDeserializer;
import org.bimserver.plugins.schema.SchemaDefinition;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class JsonIfcGeometryTreeDeserializer extends EmfDeserializer {
  private static final Logger log = Logger.getLogger(JsonIfcGeometryTreeSerializer.class.getName());

  private final WKTReader wktReader = new WKTReader();
  private final ObjectMapper mapper = new ObjectMapper();

  private static final String OBJECT_ID = "OBJECTID";
  private static final String SITE_ID = "SITEID";
  private static final String BUILDING_ID = "BUILDINGID";
  private static final String FLOOR_ID = "FLOORID";
  private static final String SPACE_ID = "SPACEID";
  private static final String BUILDING_KEY = "BUILDINGKE";
  private static final String FLOOR_KEY = "FLOORKEY";
  private static final String SPACE_KEY = "SPACEKEY";

  private static final String ADDRESS = "ADDRESS";
  private static final String BUILDING_NAME = "BUILDINGNA";
  private static final String GEOMETRY_ATTRIBUTE = "the_geom";
  private static final String MAJOR_USE = "MAJORUSE";
  private static final String SPACE_USE = "SPACEUSEDE";
  private static final String TPK = "TPK";
  private static final String FLOOR_RL = "FLOORRL";
  private static final String ROOF_RL = "ROOFRL";

  private static final String EXTERNAL_WALL = "External Wall";

  private static final double slabThickness = 0.15;

  private static final double projectLatitude = -33.88704917772104;
  private static final double projectLongitude = 151.1941205838663;

  private final Ifc2x3tc1Factory ifc2x3tc1Factory = Ifc2x3tc1Factory.eINSTANCE;
  private final GeometryFactory geometryFactory = new GeometryFactory();

  private Map<String, List<String>> parentToChildrenMap;
  private IfcModel model;
  private IfcOwnerHistory ownerHistory;
  private IfcGeometricRepresentationContext geometricRepresentationContext;

  /**
   * Maps the ID to an IfcProduct.
   */
  private Map<String, IfcProduct> idToSpatialElementMap;

  /**
   * Maps the parent IfcSpatialStructureElement to its child IfcProduct.
   */
  private Map<String, List<IfcBuildingElement>> spatialStructureContainsMap;
  private int currentOid = 1;

  @Override
  public void init(SchemaDefinition schemaDefinition) {
  }

  @Override
  public IfcModelInterface read(InputStream inputStream, String s, long l)
      throws DeserializeException {
    try {
      idToSpatialElementMap = new HashMap<>();
      spatialStructureContainsMap = new HashMap<>();
      parentToChildrenMap = new HashMap<>();

      model = new IfcModel();
      ownerHistory = createIfcOwnerHistory();

      IfcAxis2Placement3D originPlacement = ifc2x3tc1Factory.createIfcAxis2Placement3D();
      addIfcObjectToIfcModel(originPlacement);
      originPlacement.setLocation(createIfcCartesianPoint3D(0, 0, 0));

      IfcDirection directionZAxis = ifc2x3tc1Factory.createIfcDirection();
      addIfcObjectToIfcModel(directionZAxis);
      directionZAxis.eSet(getEStructuralFeatureFromName(directionZAxis, "DirectionRatios"),
          Lists.newArrayList(0.0, 0.0, 1.0));

      geometricRepresentationContext = ifc2x3tc1Factory.createIfcGeometricRepresentationContext();
      addIfcObjectToIfcModel(geometricRepresentationContext);
      geometricRepresentationContext.setCoordinateSpaceDimension(3);
      geometricRepresentationContext.setWorldCoordinateSystem(originPlacement);

      IfcProject project = createIfcProject();

      List<Map<String, String>> features = mapper.readValue(inputStream, List.class);
      List<Map<String, String>> buildingFeatures = Lists.newArrayList();
      List<Map<String, String>> buildingStoreyFeatures = Lists.newArrayList();
      List<Map<String, String>> spaceFeatures = Lists.newArrayList();
      Map<String, List<Map<String, String>>> floorIdToSpaceFeaturesMap = new HashMap<>();

      List<String> siteIds = Lists.newArrayList();
      List<String> buildingIds = Lists.newArrayList();
      List<String> storeyIds = Lists.newArrayList();
      List<String> spaceIds = Lists.newArrayList();

      List<IfcObjectDefinition> sites = Lists.newArrayList();
      for (Map<String, String> feature : features)
        if (isSite(feature)) {
          String siteId = feature.get(SITE_ID);
          String address = feature.get(ADDRESS);
          String tpk = feature.get(TPK);
          String geometryWkt = feature.get(GEOMETRY_ATTRIBUTE);

          IfcSite ifcSite = ifc2x3tc1Factory.createIfcSite();
          createIfcProductWithBasicInfo(ifcSite, ownerHistory, siteId, address, tpk);
          setLocalPlacementAndShapeRepresentationToIfcProduct(siteId, null, ifcSite, geometryWkt,
              0.1, 0);

          IfcPropertySingleValue siteEastings =
              ifc2x3tc1Factory.createIfcPropertySingleValue();
          addIfcObjectToIfcModel(siteEastings);
          siteEastings.setName("Eastings");
          IfcLengthMeasure eastingsLength = ifc2x3tc1Factory.createIfcLengthMeasure();
          eastingsLength.setWrappedValue(333600.0);
          siteEastings.setNominalValue(eastingsLength);

          IfcPropertySingleValue siteNorthings =
              ifc2x3tc1Factory.createIfcPropertySingleValue();
          addIfcObjectToIfcModel(siteNorthings);
          siteNorthings.setName("Northings");
          IfcLengthMeasure northingsLength = ifc2x3tc1Factory.createIfcLengthMeasure();
          northingsLength.setWrappedValue(6246200.0);
          siteNorthings.setNominalValue(northingsLength);

          IfcPropertySingleValue siteOrthogonalHeight =
              ifc2x3tc1Factory.createIfcPropertySingleValue();
          addIfcObjectToIfcModel(siteOrthogonalHeight);
          siteOrthogonalHeight.setName("OrthogonalHeight");
          IfcLengthMeasure orthogonalHeightLength = ifc2x3tc1Factory.createIfcLengthMeasure();
          orthogonalHeightLength.setWrappedValue(0.0);
          siteOrthogonalHeight.setNominalValue(orthogonalHeightLength);

          IfcPropertySingleValue siteXAxisAbscissa =
              ifc2x3tc1Factory.createIfcPropertySingleValue();
          addIfcObjectToIfcModel(siteXAxisAbscissa);
          siteXAxisAbscissa.setName("XAxisAbscissa");
          IfcReal xAxisAbscissa = ifc2x3tc1Factory.createIfcReal();
          xAxisAbscissa.setWrappedValue(1);
          siteXAxisAbscissa.setNominalValue(xAxisAbscissa);

          IfcPropertySingleValue siteXAxisOrdinate =
              ifc2x3tc1Factory.createIfcPropertySingleValue();
          addIfcObjectToIfcModel(siteXAxisOrdinate);
          siteXAxisOrdinate.setName("XAxisOrdinate");
          IfcReal xAxisOrdinate = ifc2x3tc1Factory.createIfcReal();
          xAxisOrdinate.setWrappedValue(0);
          siteXAxisOrdinate.setNominalValue(xAxisOrdinate);

          IfcPropertySingleValue siteScale =
              ifc2x3tc1Factory.createIfcPropertySingleValue();
          addIfcObjectToIfcModel(siteScale);
          siteScale.setName("Scale");
          IfcReal scale = ifc2x3tc1Factory.createIfcReal();
          scale.setWrappedValue(1);
          siteScale.setNominalValue(scale);

          IfcPropertySet ifcPropertySet = ifc2x3tc1Factory.createIfcPropertySet();
          addIfcObjectToIfcModel(ifcPropertySet);
          ifcPropertySet.setGlobalId(GuidCompressor.getNewIfcGloballyUniqueId());
          ifcPropertySet.setOwnerHistory(ownerHistory);
          ifcPropertySet.setName("NSW_MapConversion");
          ifcPropertySet.eSet(getEStructuralFeatureFromName(ifcPropertySet, "HasProperties"),
              Lists.newArrayList(siteEastings, siteNorthings, siteOrthogonalHeight,
                  siteXAxisAbscissa, siteXAxisOrdinate, siteScale));

          IfcRelDefinesByProperties ifcRelDefinesByProperties =
              ifc2x3tc1Factory.createIfcRelDefinesByProperties();
          addIfcObjectToIfcModel(ifcRelDefinesByProperties);
          ifcRelDefinesByProperties.setGlobalId(GuidCompressor.getNewIfcGloballyUniqueId());
          ifcRelDefinesByProperties.setOwnerHistory(ownerHistory);
          ifcRelDefinesByProperties.eSet(
              getEStructuralFeatureFromName(ifcRelDefinesByProperties, "RelatedObjects"),
              Lists.newArrayList(ifcSite));
          ifcRelDefinesByProperties.setRelatingPropertyDefinition(ifcPropertySet);

          sites.add(ifcSite);
          siteIds.add(siteId);
        } else if (isBuilding(feature)) {
          buildingFeatures.add(feature);
        } else if (isFloor(feature)) {
          String spaceId = feature.get(SPACE_ID);
          if (Strings.isNullOrEmpty(spaceId)) { // If it has spaceId, it is not a storey.
            buildingStoreyFeatures.add(feature);
          }
        } else if (isSpace(feature)) {
          String floorKey = feature.get(FLOOR_KEY);
          List<Map<String, String>> spaceFeature = floorIdToSpaceFeaturesMap.get(floorKey);
          if (spaceFeature == null) {
            spaceFeature = Lists.newArrayList();
            floorIdToSpaceFeaturesMap.put(floorKey, spaceFeature);
          }
          spaceFeature.add(feature);
        }

      for (Map<String, String> buildingFeature : buildingFeatures) {
        String siteId = buildingFeature.get(SITE_ID);
        String buildingKey = buildingFeature.get(BUILDING_KEY);
        String buildingName = buildingFeature.get(BUILDING_NAME);
        String majorUse = buildingFeature.get(MAJOR_USE);
        String geometryWkt = buildingFeature.get(GEOMETRY_ATTRIBUTE);

        IfcBuilding ifcBuilding = ifc2x3tc1Factory.createIfcBuilding();
        if (Strings.isNullOrEmpty(buildingName)) {
          buildingName = majorUse;
        }
        createIfcProductWithBasicInfo(ifcBuilding, ownerHistory, buildingKey, buildingName,
            majorUse);
        setLocalPlacementAndShapeRepresentationToIfcProduct(buildingKey, siteId, ifcBuilding,
            geometryWkt, 0.1, 0);

        addChildToParent(siteId, buildingKey);
        buildingIds.add(buildingKey);
      }

      for (Map<String, String> buildingStoreyFeature : buildingStoreyFeatures) {
        String siteId = buildingStoreyFeature.get(SITE_ID);
        String buildingKey = buildingStoreyFeature.get(BUILDING_KEY);
        if (Strings.isNullOrEmpty(buildingKey)) {
          String buildingId = buildingStoreyFeature.get(BUILDING_ID);
          buildingKey = siteId.concat(buildingId);
        }
        String floorKey = buildingStoreyFeature.get(FLOOR_KEY);
        if (Strings.isNullOrEmpty(floorKey)) {
          String floorId = buildingStoreyFeature.get(FLOOR_ID);
          floorKey = buildingKey.concat(floorId);
        }
        String floorLevelString = buildingStoreyFeature.get(FLOOR_RL);
        String roofLevelString = buildingStoreyFeature.get(ROOF_RL);
        double floorLevel = Double.parseDouble(floorLevelString);
        double roofLevel = Double.parseDouble(roofLevelString);
        String geometryWkt = buildingStoreyFeature.get(GEOMETRY_ATTRIBUTE);
        double height = roofLevel - floorLevel;

        if (geometryWkt != null) {
          log.info("Floor has geometry");
        }

        IfcBuildingStorey ifcBuildingStorey = ifc2x3tc1Factory.createIfcBuildingStorey();
        createIfcProductWithBasicInfo(ifcBuildingStorey, ownerHistory, floorKey, floorKey, "");
        setLocalPlacementAndShapeRepresentationToIfcProduct(floorKey, buildingKey,
            ifcBuildingStorey, geometryWkt, 0.1, 0);

        addChildToParent(buildingKey, floorKey);
        storeyIds.add(floorKey);
      }

      for (String floorKey : floorIdToSpaceFeaturesMap.keySet()) {
        List<Map<String, String>> spaceFeaturesPerFloor = floorIdToSpaceFeaturesMap.get(floorKey);
        Double maxFloorLevel = null, maxRoofLevel = null;
        String buildingKey = null;
        for (Map<String, String> spaceFeature : spaceFeaturesPerFloor) {
          buildingKey = spaceFeature.get("BuildingKe");
          String spaceUse = spaceFeature.get(SPACE_USE);
          String floorLevelString = spaceFeature.get("FloorRL");
          String roofLevelString = spaceFeature.get("RoofRL");

          if (maxFloorLevel == null) {
            maxFloorLevel = Double.parseDouble(floorLevelString);
          }
          if (spaceUse.equals(EXTERNAL_WALL)) {
            maxFloorLevel = Double.parseDouble(floorLevelString);
            maxRoofLevel = Double.parseDouble(roofLevelString);
          }
        }
        if (!storeyIds.contains(floorKey)) {
          IfcBuildingStorey ifcBuildingStorey = ifc2x3tc1Factory.createIfcBuildingStorey();
          createIfcProductWithBasicInfo(ifcBuildingStorey, ownerHistory, floorKey, floorKey, "");
          setLocalPlacementAndShapeRepresentationToIfcProduct(floorKey, buildingKey,
              ifcBuildingStorey, null, 0.1, maxFloorLevel);
          ifcBuildingStorey.setElevation(maxFloorLevel);

          addChildToParent(buildingKey, floorKey);
          storeyIds.add(floorKey);
        }
        if (maxFloorLevel != null) {
          maxFloorLevel += slabThickness;
        }
        if (maxRoofLevel != null) {
          maxRoofLevel -= slabThickness;
        }
        for (Map<String, String> spaceFeature : spaceFeaturesPerFloor) {
          String spaceKey = spaceFeature.get(SPACE_KEY);
          String spaceUse = spaceFeature.get(SPACE_USE);
          String floorLevelString = spaceFeature.get("FloorRL");
          String roofLevelString = spaceFeature.get("RoofRL");
          String geometryWkt = spaceFeature.get(GEOMETRY_ATTRIBUTE);
          double floorLevel = Double.parseDouble(floorLevelString);
          double roofLevel = Double.parseDouble(roofLevelString);
          if (maxFloorLevel != null) {
            if (floorLevel < maxFloorLevel) {
              floorLevel = maxFloorLevel;
            }
          }
          if (maxRoofLevel != null) {
            if (roofLevel > maxRoofLevel) {
              roofLevel = maxRoofLevel;
            }
          }
          double height = roofLevel - floorLevel;

          if (spaceUse.equals(EXTERNAL_WALL)) {
            Geometry geometry = wktReader.read(geometryWkt);
            String exteriorRingWkt = geometryWkt;
            if (geometry instanceof Polygon) {
              Polygon polygon = (Polygon) geometry;
              exteriorRingWkt = polygon.getExteriorRing().toString();
            }

            List<IfcBuildingElement> buildingElements = Lists.newArrayList();

            IfcRoof ifcRoof = ifc2x3tc1Factory.createIfcRoof();
            createIfcProductWithBasicInfo(ifcRoof, ownerHistory, floorKey, "Roof", "");
            setLocalPlacementAndShapeRepresentationToIfcProduct(floorKey + ".Roof", floorKey,
                ifcRoof, exteriorRingWkt, slabThickness, height + slabThickness);
            ifcRoof.setShapeType(IfcRoofTypeEnum.FLAT_ROOF);
            buildingElements.add(ifcRoof);

            IfcSlab ifcSlab = ifc2x3tc1Factory.createIfcSlab();
            createIfcProductWithBasicInfo(ifcSlab, ownerHistory, floorKey, "Floor", "");
            setLocalPlacementAndShapeRepresentationToIfcProduct(floorKey + ".Floor", floorKey,
                ifcSlab, exteriorRingWkt, slabThickness, 0);
            ifcSlab.setPredefinedType(IfcSlabTypeEnum.FLOOR);
            buildingElements.add(ifcSlab);

            IfcWall ifcWall = ifc2x3tc1Factory.createIfcWall();
            createIfcProductWithBasicInfo(ifcWall, ownerHistory, floorKey, "ExternalWall", "");
            setLocalPlacementAndShapeRepresentationToIfcProduct(floorKey + ".Wall", floorKey,
                ifcWall, geometryWkt, height, slabThickness);
            buildingElements.add(ifcWall);

            List<IfcBuildingElement> ifcBuildingElements =
                spatialStructureContainsMap.get(floorKey);
            if (ifcBuildingElements == null) {
              ifcBuildingElements = Lists.newArrayList();
              spatialStructureContainsMap.put(floorKey, ifcBuildingElements);
            }
            ifcBuildingElements.addAll(buildingElements);
            continue;
          }

          if (spaceIds.contains(spaceKey)) {
            spaceKey = createNextFeatureId(spaceIds, spaceKey);
          }
          spaceIds.add(spaceKey);
          IfcSpace ifcSpace = ifc2x3tc1Factory.createIfcSpace();
          createIfcProductWithBasicInfo(ifcSpace, ownerHistory, spaceKey, spaceKey, spaceUse);
          setLocalPlacementAndShapeRepresentationToIfcProduct(spaceKey, floorKey, ifcSpace,
              geometryWkt, height, slabThickness);

          addChildToParent(floorKey, spaceKey);
        }
      }

      createIfcRelAggregates(ownerHistory, project, sites);

      for (String parentId : parentToChildrenMap.keySet()) {
        IfcProduct parentElement = idToSpatialElementMap.get(parentId);
        if (parentElement == null) {
          log.info("Aggregates: Unable to find parent with id: " + parentId);
          continue;
        }
        List<String> children = parentToChildrenMap.get(parentId);
        List<IfcObjectDefinition> childElements = Lists.newArrayList();
        for (String childId : children) {
          IfcProduct childElement = idToSpatialElementMap.get(childId);
          if (childElement == null) {
            log.info("Unable to find child with id: " + childId);
          }
          childElements.add(childElement);
        }
        createIfcRelAggregates(ownerHistory, parentElement, childElements);
      }

      for (String parentId : spatialStructureContainsMap.keySet()) {
        IfcProduct parentElement = idToSpatialElementMap.get(parentId);
        if (parentElement == null) {
          log.info("Contains: Unable to find parent with id: " + parentId);
          continue;
        }
        if (!(parentElement instanceof IfcSpatialStructureElement)) {
          log.info("Contains: Parent element is not a spatial structure: " + parentId);
          continue;
        }
        IfcSpatialStructureElement spatialStructureElement =
            (IfcSpatialStructureElement) parentElement;
        List<IfcBuildingElement> ifcBuildingElements = spatialStructureContainsMap.get(parentId);
        IfcRelContainedInSpatialStructure relContainedInSpatialStructure =
            ifc2x3tc1Factory.createIfcRelContainedInSpatialStructure();
        addIfcObjectToIfcModel(relContainedInSpatialStructure);
        relContainedInSpatialStructure.setGlobalId(GuidCompressor.getNewIfcGloballyUniqueId());
        relContainedInSpatialStructure.setOwnerHistory(ownerHistory);
        relContainedInSpatialStructure.setRelatingStructure(spatialStructureElement);
        relContainedInSpatialStructure.eSet(getEStructuralFeatureFromName(relContainedInSpatialStructure,
            "RelatedElements"), ifcBuildingElements);
      }

      return model;
    }catch (IfcModelInterfaceException | IOException | ParseException e) {
      log.severe(e.getMessage());
    } finally {
    }
    return null;
  }

  private void setLocalPlacementAndShapeRepresentationToIfcProduct(String id, String parentId,
      IfcProduct ifcProduct, String geometryWkt, double height, double altitude)
      throws IfcModelInterfaceException {
    IfcAxis2Placement3D ifcAxis2Placement3D = ifc2x3tc1Factory.createIfcAxis2Placement3D();
    addIfcObjectToIfcModel(ifcAxis2Placement3D);
    ifcAxis2Placement3D.setLocation(createIfcCartesianPoint3D(0, 0, altitude));

    IfcLocalPlacement ifcLocalPlacement = ifc2x3tc1Factory.createIfcLocalPlacement();
    addIfcObjectToIfcModel(ifcLocalPlacement);
    if (parentId != null) {
      IfcProduct parentElement = idToSpatialElementMap.get(parentId);
      if (parentElement == null) {
        log.info("Unable to find site with id: " + parentId);
        return;
      }
      ifcLocalPlacement.setPlacementRelTo(parentElement.getObjectPlacement());
    }
    ifcLocalPlacement.setRelativePlacement(ifcAxis2Placement3D);

    ifcProduct.setObjectPlacement(ifcLocalPlacement);

    if (!Strings.isNullOrEmpty(geometryWkt)) {
      IfcProductDefinitionShape ifcProductRepresentation =
          createIfcProductRepresentation(geometryWkt, height);
      ifcProduct.setRepresentation(ifcProductRepresentation);
    }
  }

  private String createNextFeatureId(List<String> existingIds, String id) {
    int count = 1;
    boolean foundNewId = false;
    while (!foundNewId) {
      String newId = id + "." + count;
      if (!existingIds.contains(newId)) {
        return newId;
      }
      count++;
    }
    return null;
  }

  private void createIfcProductWithBasicInfo(IfcProduct product,
      IfcOwnerHistory ownerHistory, String id, String name, String fullName)
      throws IfcModelInterfaceException {
    addIfcObjectToIfcModel(product);
    product.setGlobalId(GuidCompressor.getNewIfcGloballyUniqueId());
    product.setOwnerHistory(ownerHistory);
    product.setName(name);
    if (product instanceof IfcSpatialStructureElement) {
      IfcSpatialStructureElement spatialStructureElement = (IfcSpatialStructureElement) product;
      spatialStructureElement.setLongName(fullName);
      spatialStructureElement.setCompositionType(IfcElementCompositionEnum.ELEMENT);
      idToSpatialElementMap.put(id, product);
    } else {
      product.setDescription(fullName);
    }
  }

  private IfcProductDefinitionShape createIfcProductRepresentation(String geometryWkt,
      double height) throws IfcModelInterfaceException {
    if (!Strings.isNullOrEmpty(geometryWkt)) {
      IfcProductDefinitionShape extrudedPolygonShape =
          createIfcProductDefinitionShapeExtrudedPolygon(geometryWkt, height);
      return extrudedPolygonShape;
    }
    return null;
  }

  private void createIfcRelAggregates(IfcOwnerHistory ownerHistory,
      IfcObjectDefinition parentElement, List<IfcObjectDefinition> childElements)
      throws IfcModelInterfaceException {
    IfcRelAggregates relAggregates = ifc2x3tc1Factory.createIfcRelAggregates();
    addIfcObjectToIfcModel(relAggregates);
    relAggregates.setGlobalId(GuidCompressor.getNewIfcGloballyUniqueId());
    relAggregates.setOwnerHistory(ownerHistory);
    relAggregates.setRelatingObject(parentElement);
    relAggregates.eSet(getEStructuralFeatureFromName(relAggregates, "RelatedObjects"),
        childElements);

    parentElement.eSet(getEStructuralFeatureFromName(parentElement, "IsDecomposedBy"),
        Lists.newArrayList(relAggregates));
  }

  private void addChildToParent(String parentId, String childId) {
    List<String> children = parentToChildrenMap.get(parentId);
    if (children == null) {
      children = Lists.newArrayList();
      parentToChildrenMap.put(parentId, children);
    }
    children.add(childId);
  }

  private IfcOwnerHistory createIfcOwnerHistory()
      throws IfcModelInterfaceException {
    IfcPerson ifcPerson = ifc2x3tc1Factory.createIfcPerson();
    addIfcObjectToIfcModel(ifcPerson);
    ifcPerson.setId("Undefined");

    IfcOrganization ifcOrganization = ifc2x3tc1Factory.createIfcOrganization();
    addIfcObjectToIfcModel(ifcOrganization);
    ifcOrganization.setName("Deltarch");

    IfcPersonAndOrganization ifcPersonAndOrganization =
        ifc2x3tc1Factory.createIfcPersonAndOrganization();
    addIfcObjectToIfcModel(ifcPersonAndOrganization);
    ifcPersonAndOrganization.setThePerson(ifcPerson);
    ifcPersonAndOrganization.setTheOrganization(ifcOrganization);
    ifcPersonAndOrganization.unsetRoles();

    IfcApplication ifcApplication = ifc2x3tc1Factory.createIfcApplication();
    addIfcObjectToIfcModel(ifcApplication);
    ifcApplication.setApplicationDeveloper(ifcOrganization);
    ifcApplication.setVersion("1.0.0");
    ifcApplication.setApplicationFullName("Asset Conversion Service");
    ifcApplication.setApplicationIdentifier("ACS");

    IfcOwnerHistory ifcOwnerHistory = ifc2x3tc1Factory.createIfcOwnerHistory();
    addIfcObjectToIfcModel(ifcOwnerHistory);
    ifcOwnerHistory.setOwningUser(ifcPersonAndOrganization);
    ifcOwnerHistory.setOwningApplication(ifcApplication);
    ifcOwnerHistory.setChangeAction(IfcChangeActionEnum.NOCHANGE);
    ifcOwnerHistory.setCreationDate(1331682111);

    return ifcOwnerHistory;
  }

  private IfcProject createIfcProject()
      throws IfcModelInterfaceException {
    IfcSIUnit ifcLengthSIUnit = ifc2x3tc1Factory.createIfcSIUnit();
    addIfcObjectToIfcModel(ifcLengthSIUnit);
    ifcLengthSIUnit.setUnitType(IfcUnitEnum.LENGTHUNIT);
//    ifcLengthSIUnit.setPrefix(IfcSIPrefix.MILLI);
    ifcLengthSIUnit.setName(IfcSIUnitName.METRE);

    IfcUnitAssignment ifcUnitAssignment = ifc2x3tc1Factory.createIfcUnitAssignment();
    addIfcObjectToIfcModel(ifcUnitAssignment);
    ifcUnitAssignment.eSet(getEStructuralFeatureFromName(ifcUnitAssignment, "Units"),
        Lists.newArrayList(ifcLengthSIUnit));

    IfcProject ifcProject = ifc2x3tc1Factory.createIfcProject();
    addIfcObjectToIfcModel(ifcProject);
    ifcProject.setGlobalId(GuidCompressor.getNewIfcGloballyUniqueId());
    ifcProject.setOwnerHistory(ownerHistory);
    ifcProject.setName("BroadwayFSES");
    ifcProject.setDescription("Broadway FSES project");
    ifcProject.eSet(getEStructuralFeatureFromName(ifcProject, "RepresentationContexts"),
        Lists.newArrayList(geometricRepresentationContext));
    ifcProject.setUnitsInContext(ifcUnitAssignment);

    return ifcProject;
  }

  private IfcProductDefinitionShape createIfcProductDefinitionShapeExtrudedPolygon(String wkt,
      double height) {
    try {
      Geometry geometry = wktReader.read(wkt);
      if (geometry instanceof Polygon) {
        Polygon polygon = (Polygon) geometry;
        IfcExtrudedAreaSolid ifcExtrudedAreaSolid =
            createIfcExtrudeAreaSolid(height, polygon.getExteriorRing().getCoordinates());

        IfcGeometricRepresentationItem ifcGeometricRepresentationItem = ifcExtrudedAreaSolid;
        String representationType = "SweptSolid";

//        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
//          LineString interiorRing = polygon.getInteriorRingN(i);
        if (polygon.getNumInteriorRing() > 1) {
          log.info("More than 1 interior ring.");
        }
        if (polygon.getNumInteriorRing() > 0) {
          LineString interiorRing = polygon.getInteriorRingN(0);
          List<IfcCartesianPoint> points = Lists.newArrayList();
          for (Coordinate coordinate : interiorRing.getCoordinates()) {
            IfcCartesianPoint point = createIfcCartesianPoint2D(coordinate.y, coordinate.x);
            points.add(point);
          }

          IfcPolyline ifcPolyline = ifc2x3tc1Factory.createIfcPolyline();
          addIfcObjectToIfcModel(ifcPolyline);
          ifcPolyline.eSet(getEStructuralFeatureFromName(ifcPolyline, "Points"), points);

          IfcDirection ifcDirection = ifc2x3tc1Factory.createIfcDirection();
          addIfcObjectToIfcModel(ifcDirection);
          ifcDirection.eSet(getEStructuralFeatureFromName(ifcDirection, "DirectionRatios"),
              Lists.newArrayList(0.0, 0.0, 0.0));

          IfcAxis2Placement3D ifcAxis2Placement3D = ifc2x3tc1Factory.createIfcAxis2Placement3D();
          addIfcObjectToIfcModel(ifcAxis2Placement3D);
          ifcAxis2Placement3D.setLocation(createIfcCartesianPoint3D(0, 0, 0));
          ifcAxis2Placement3D.setAxis(ifcDirection);

          IfcPlane ifcPlane = ifc2x3tc1Factory.createIfcPlane();
          addIfcObjectToIfcModel(ifcPlane);
          ifcPlane.setPosition(ifcAxis2Placement3D);

          IfcAxis2Placement3D originPlacement = createOriginAxis2Placement3D();

          IfcPolygonalBoundedHalfSpace ifcPolygonalBoundedHalfSpace =
              ifc2x3tc1Factory.createIfcPolygonalBoundedHalfSpace();
          addIfcObjectToIfcModel(ifcPolygonalBoundedHalfSpace);
          ifcPolygonalBoundedHalfSpace.setBaseSurface(ifcPlane);
          ifcPolygonalBoundedHalfSpace.setAgreementFlag(Tristate.FALSE);
          ifcPolygonalBoundedHalfSpace.setPosition(originPlacement);
          ifcPolygonalBoundedHalfSpace.setPolygonalBoundary(ifcPolyline);

          IfcBooleanClippingResult ifcBooleanClippingResult =
              ifc2x3tc1Factory.createIfcBooleanClippingResult();
          addIfcObjectToIfcModel(ifcBooleanClippingResult);
          ifcBooleanClippingResult.setFirstOperand(ifcExtrudedAreaSolid);
          ifcBooleanClippingResult.setSecondOperand(ifcPolygonalBoundedHalfSpace);
          ifcBooleanClippingResult.setOperator(IfcBooleanOperator.DIFFERENCE);

          ifcGeometricRepresentationItem = ifcBooleanClippingResult;
          representationType = "Clipping";
        }

        IfcShapeRepresentation ifcShapeRepresentation =
            ifc2x3tc1Factory.createIfcShapeRepresentation();
        addIfcObjectToIfcModel(ifcShapeRepresentation);
        ifcShapeRepresentation.setContextOfItems(geometricRepresentationContext);
        ifcShapeRepresentation.setRepresentationIdentifier("Body");
        ifcShapeRepresentation.setRepresentationType(representationType);
        ifcShapeRepresentation.eSet(getEStructuralFeatureFromName(ifcShapeRepresentation, "Items"),
            Lists.newArrayList(ifcGeometricRepresentationItem));

        IfcProductDefinitionShape ifcProductDefinitionShape =
            ifc2x3tc1Factory.createIfcProductDefinitionShape();
        addIfcObjectToIfcModel(ifcProductDefinitionShape);
        ifcProductDefinitionShape.eSet(
            getEStructuralFeatureFromName(ifcProductDefinitionShape, "Representations"),
            Lists.newArrayList(ifcShapeRepresentation));

        return ifcProductDefinitionShape;
      }
      IfcExtrudedAreaSolid ifcExtrudedAreaSolid =
          createIfcExtrudeAreaSolid(height, geometry.getCoordinates());

      IfcShapeRepresentation ifcShapeRepresentation =
          ifc2x3tc1Factory.createIfcShapeRepresentation();
      addIfcObjectToIfcModel(ifcShapeRepresentation);
      ifcShapeRepresentation.setContextOfItems(geometricRepresentationContext);
      ifcShapeRepresentation.setRepresentationIdentifier("Body");
      ifcShapeRepresentation.setRepresentationType("SweptSolid");
      ifcShapeRepresentation.eSet(getEStructuralFeatureFromName(ifcShapeRepresentation, "Items"),
          Lists.newArrayList(ifcExtrudedAreaSolid));

      IfcProductDefinitionShape ifcProductDefinitionShape =
          ifc2x3tc1Factory.createIfcProductDefinitionShape();
      addIfcObjectToIfcModel(ifcProductDefinitionShape);
      ifcProductDefinitionShape.eSet(
          getEStructuralFeatureFromName(ifcProductDefinitionShape, "Representations"),
          Lists.newArrayList(ifcShapeRepresentation));

      return ifcProductDefinitionShape;
    } catch (ParseException | IfcModelInterfaceException e) {
      log.severe(e.getMessage());
    }
    return null;
  }


  private IfcExtrudedAreaSolid createIfcExtrudeAreaSolid(double height,
      Coordinate[] coordinates) throws IfcModelInterfaceException {
    List<IfcCartesianPoint> points = Lists.newArrayList();
    for (Coordinate coordinate : coordinates) {
      IfcCartesianPoint point = createIfcCartesianPoint2D(coordinate.y, coordinate.x);
      points.add(point);
    }

    IfcPolyline ifcPolyline = ifc2x3tc1Factory.createIfcPolyline();
    addIfcObjectToIfcModel(ifcPolyline);
    ifcPolyline.eSet(getEStructuralFeatureFromName(ifcPolyline, "Points"), points);

    IfcArbitraryClosedProfileDef ifcArbitraryClosedProfileDef =
        ifc2x3tc1Factory.createIfcArbitraryClosedProfileDef();
    addIfcObjectToIfcModel(ifcArbitraryClosedProfileDef);
    ifcArbitraryClosedProfileDef.setProfileType(IfcProfileTypeEnum.AREA);
    ifcArbitraryClosedProfileDef.setOuterCurve(ifcPolyline);

    IfcAxis2Placement3D originPlacement = createOriginAxis2Placement3D();
    IfcDirection zAxisDirection = createZAxisDirection();

    IfcExtrudedAreaSolid ifcExtrudedAreaSolid = ifc2x3tc1Factory.createIfcExtrudedAreaSolid();
    addIfcObjectToIfcModel(ifcExtrudedAreaSolid);
    ifcExtrudedAreaSolid.setSweptArea(ifcArbitraryClosedProfileDef);
    ifcExtrudedAreaSolid.setPosition(originPlacement);
    ifcExtrudedAreaSolid.setDepth(height);
    ifcExtrudedAreaSolid.setExtrudedDirection(zAxisDirection);

    return ifcExtrudedAreaSolid;
  }

  private List<Coordinate> getClosedPolygon(Coordinate[] coordinates) {
    List<Coordinate> coordinateList = Lists.newArrayList();
    for (int i = 0; i < coordinates.length; i++) {
      coordinateList.add(coordinates[i]);
    }
    return coordinateList;
  }

  private void addIfcObjectToIfcModel(IdEObject object)
      throws IfcModelInterfaceException {
    ((IdEObjectImpl) object).setExpressId(currentOid);
    model.add(currentOid, object);
    currentOid++;
  }

  private EStructuralFeature getEStructuralFeatureFromName(IdEObject object, String featureName) {
    EStructuralFeature eStructuralFeature = object.eClass().getEStructuralFeature(featureName);
    return eStructuralFeature;
  }

  private IfcAxis2Placement3D createOriginAxis2Placement3D() throws IfcModelInterfaceException {
    IfcAxis2Placement3D originPlacement = ifc2x3tc1Factory.createIfcAxis2Placement3D();
    addIfcObjectToIfcModel(originPlacement);
    originPlacement.setLocation(createIfcCartesianPoint3D(0, 0, 0));
    return originPlacement;
  }

  private IfcDirection createZAxisDirection() throws IfcModelInterfaceException {
    IfcDirection zAxisDirection = ifc2x3tc1Factory.createIfcDirection();
    addIfcObjectToIfcModel(zAxisDirection);
    zAxisDirection.eSet(getEStructuralFeatureFromName(zAxisDirection, "DirectionRatios"),
        Lists.newArrayList(0.0, 0.0, 1.0));
    return zAxisDirection;
  }

  private IfcCartesianPoint createIfcCartesianPoint3D(double x, double y,
      double z) throws IfcModelInterfaceException {
    IfcCartesianPoint ifcCartesianPoint = ifc2x3tc1Factory.createIfcCartesianPoint();
    addIfcObjectToIfcModel(ifcCartesianPoint);
    ifcCartesianPoint.eSet(getEStructuralFeatureFromName(ifcCartesianPoint, "Coordinates"),
        Lists.newArrayList(x, y, z));
    return ifcCartesianPoint;
  }

  private IfcCartesianPoint createIfcCartesianPoint2D(double x, double y)
      throws IfcModelInterfaceException {
    IfcCartesianPoint ifcCartesianPoint = ifc2x3tc1Factory.createIfcCartesianPoint();
    addIfcObjectToIfcModel(ifcCartesianPoint);
    ifcCartesianPoint.eSet(getEStructuralFeatureFromName(ifcCartesianPoint, "Coordinates"),
        Lists.newArrayList(x, y));
    return ifcCartesianPoint;
  }

  private boolean isSite(Map<String, String> feature) {
    return !feature.containsKey(BUILDING_KEY) && !feature.containsKey(FLOOR_KEY) &&
        !feature.containsKey(SPACE_KEY);
  }

  private boolean isBuilding(Map<String, String> feature) {
    return feature.containsKey(BUILDING_KEY) && !feature.containsKey(FLOOR_KEY) &&
        !feature.containsKey(SPACE_KEY);
  }

  private boolean isFloor(Map<String, String> feature) {
    return feature.containsKey(FLOOR_KEY) && !feature.containsKey(SPACE_KEY);
  }

  private boolean isSpace(Map<String, String> feature) {
    return feature.containsKey(SPACE_KEY);
  }
}
