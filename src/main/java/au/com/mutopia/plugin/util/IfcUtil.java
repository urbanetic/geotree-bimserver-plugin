package au.com.mutopia.plugin.util;

import java.util.Map;
import java.util.logging.Logger;
import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.ifc2x3tc1.*;
import org.bimserver.models.store.SIPrefix;
import org.eclipse.emf.common.util.EList;

/**
 * IFC util class that contains many common methods for accessing IFC objects.
 */
public class IfcUtil {
  private static final Logger log = Logger.getLogger(IfcUtil.class.getName());

  /**
   * See http://www.buildingsmart-tech.org/ifc/IFC2x3/TC1/html/ifcmeasureresource/lexical/ifcvalue.htm
   * for list of different {@link IfcValue} types.
   *
   * @param ifcValue
   * @return The string value from different {@link IfcValue} types.
   */
  public String getStringValueFromIfcValue(IfcValue ifcValue) {
    if (ifcValue instanceof IfcSimpleValue) {
      return getWrappedValueFromIfcSimpleValue((IfcSimpleValue) ifcValue);
    } else if (ifcValue instanceof IfcMeasureValue) {
      return getWrappedValueFromIfcMeasureValue((IfcMeasureValue) ifcValue);
    } else if (ifcValue instanceof IfcDerivedMeasureValue) {
      return getWrappedValueFromIfcDerivedMeasureValue((IfcDerivedMeasureValue) ifcValue);
    } else {
      log.info("Unknown IfcPropertySingleValue value : " + ifcValue.toString() + " " + ifcValue);
    }
    return null;
  }

  /**
   * See http://www.buildingsmart-tech.org/ifc/IFC2x3/TC1/html/ifcmeasureresource/lexical/ifcsimplevalue.htm
   * for list of different {@link IfcSimpleValue} types.
   *
   * @param simpleValue
   * @return The string value from different {@link IfcSimpleValue} types.
   */
  public String getWrappedValueFromIfcSimpleValue(IfcSimpleValue simpleValue) {
    if (simpleValue instanceof IfcInteger) {
      IfcInteger integer = (IfcInteger) simpleValue;
      return "" + integer.getWrappedValue();
    } else if (simpleValue instanceof IfcReal) {
      IfcReal real = (IfcReal) simpleValue;
      return "" + real.getWrappedValue();
    } else if (simpleValue instanceof IfcBoolean) {
      IfcBoolean booleanValue = (IfcBoolean) simpleValue;
      return "" + booleanValue.getWrappedValue();
    } else if (simpleValue instanceof IfcIdentifier) {
      IfcIdentifier identifier = (IfcIdentifier) simpleValue;
      return "" + identifier.getWrappedValue();
    } else if (simpleValue instanceof IfcText) {
      IfcText text = (IfcText) simpleValue;
      return "" + text.getWrappedValue();
    } else if (simpleValue instanceof IfcLabel) {
      IfcLabel label = (IfcLabel) simpleValue;
      return "" + label.getWrappedValue();
    } else if (simpleValue instanceof IfcLogical) {
      IfcLogical logical = (IfcLogical) simpleValue;
      return "" + logical.getWrappedValue();
    } else {
      log.info("Unknown IfcSimpleValue value : " + simpleValue.toString());
    }
    return null;
  }

  /**
   * See http://www.buildingsmart-tech.org/ifc/IFC2x3/TC1/html/ifcmeasureresource/lexical/ifcmeasurevalue.htm
   * for list of different {@link IfcMeasureValue} types.
   *
   * @param measureValue
   * @return The string value from different {@link IfcMeasureValue} types.
   */
  public String getWrappedValueFromIfcMeasureValue(IfcMeasureValue measureValue) {
    if (measureValue instanceof IfcVolumeMeasure) {
      IfcVolumeMeasure volumeMeasure = (IfcVolumeMeasure) measureValue;
      return "" + volumeMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcTimeMeasure) {
      IfcTimeMeasure timeMeasure = (IfcTimeMeasure) measureValue;
      return "" + timeMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcThermodynamicTemperatureMeasure) {
      IfcThermodynamicTemperatureMeasure thermodynamicTemperatureMeasure =
          (IfcThermodynamicTemperatureMeasure) measureValue;
      return "" + thermodynamicTemperatureMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcSolidAngleMeasure) {
      IfcSolidAngleMeasure solidAngleMeasure = (IfcSolidAngleMeasure) measureValue;
      return "" + solidAngleMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcPositiveRatioMeasure) {
      IfcPositiveRatioMeasure positiveRatioMeasure = (IfcPositiveRatioMeasure) measureValue;
      return "" + positiveRatioMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcRatioMeasure) {
      IfcRatioMeasure ratioMeasure = (IfcRatioMeasure) measureValue;
      return "" + ratioMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcPositivePlaneAngleMeasure) {
      IfcPositivePlaneAngleMeasure positivePlaneAngleMeasure =
          (IfcPositivePlaneAngleMeasure) measureValue;
      return "" + positivePlaneAngleMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcPlaneAngleMeasure) {
      IfcPlaneAngleMeasure planeAngleMeasure = (IfcPlaneAngleMeasure) measureValue;
      return "" + planeAngleMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcParameterValue) {
      IfcParameterValue parameterValue = (IfcParameterValue) measureValue;
      return "" + parameterValue.getWrappedValue();
    } else if (measureValue instanceof IfcNumericMeasure) {
      IfcNumericMeasure numericMeasure = (IfcNumericMeasure) measureValue;
      return "" + numericMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcMassMeasure) {
      IfcMassMeasure massMeasure = (IfcMassMeasure) measureValue;
      return "" + massMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcPositiveLengthMeasure) {
      IfcPositiveLengthMeasure positiveLengthMeasure = (IfcPositiveLengthMeasure) measureValue;
      return "" + positiveLengthMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcLengthMeasure) {
      IfcLengthMeasure lengthMeasure = (IfcLengthMeasure) measureValue;
      return "" + lengthMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcElectricCurrentMeasure) {
      IfcElectricCurrentMeasure electricCurrentMeasure = (IfcElectricCurrentMeasure) measureValue;
      return "" + electricCurrentMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcDescriptiveMeasure) {
      IfcDescriptiveMeasure descriptiveMeasure = (IfcDescriptiveMeasure) measureValue;
      return "" + descriptiveMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcCountMeasure) {
      IfcCountMeasure countMeasure = (IfcCountMeasure) measureValue;
      return "" + countMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcContextDependentMeasure) {
      IfcContextDependentMeasure contextDependentMeasure = (IfcContextDependentMeasure) measureValue;
      return "" + contextDependentMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcAreaMeasure) {
      IfcAreaMeasure areaMeasure = (IfcAreaMeasure) measureValue;
      return "" + areaMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcAmountOfSubstanceMeasure) {
      IfcAmountOfSubstanceMeasure substanceMeasure = (IfcAmountOfSubstanceMeasure) measureValue;
      return "" + substanceMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcLuminousIntensityMeasure) {
      IfcLuminousIntensityMeasure luminousIntensityMeasure =
          (IfcLuminousIntensityMeasure) measureValue;
      return "" + luminousIntensityMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcNormalisedRatioMeasure) {
      IfcNormalisedRatioMeasure normalisedRatioMeasure = (IfcNormalisedRatioMeasure) measureValue;
      return "" + normalisedRatioMeasure.getWrappedValue();
    } else if (measureValue instanceof IfcComplexNumber) {
      IfcComplexNumber complexNumber = (IfcComplexNumber) measureValue;
      return "" + complexNumber.getWrappedValue();
    } else {
      log.info("Unknown IfcMeasureValue value : " + measureValue.toString());
    }
    return null;
  }

  /**
   * See http://www.buildingsmart-tech.org/ifc/IFC2x3/TC1/html/ifcmeasureresource/lexical/ifcderivedmeasurevalue.htm
   * for list of different {@link org.bimserver.models.ifc2x3tc1.IfcDerivedMeasureValue} types.
   *
   * @param derivedMeasureValue
   * @return The string value from different {@link org.bimserver.models.ifc2x3tc1.IfcDerivedMeasureValue} types.
   */
  public String  getWrappedValueFromIfcDerivedMeasureValue(
      IfcDerivedMeasureValue derivedMeasureValue) {
    if (derivedMeasureValue instanceof IfcElectricCapacitanceMeasure) {
      return "" + ((IfcElectricCapacitanceMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcElectricVoltageMeasure) {
      return "" + ((IfcElectricVoltageMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcEnergyMeasure) {
      return "" + ((IfcEnergyMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcIlluminanceMeasure) {
      return "" + ((IfcIlluminanceMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcIsothermalMoistureCapacityMeasure) {
      return "" + ((IfcIsothermalMoistureCapacityMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcLuminousFluxMeasure) {
      return "" + ((IfcLuminousFluxMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcMassFlowRateMeasure) {
      return "" + ((IfcMassFlowRateMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcMoistureDiffusivityMeasure) {
      return "" + ((IfcMoistureDiffusivityMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcPowerMeasure) {
      return "" + ((IfcPowerMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcPressureMeasure) {
      return "" + ((IfcPressureMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcThermalConductivityMeasure) {
      return "" + ((IfcThermalConductivityMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcThermalResistanceMeasure) {
      return "" + ((IfcThermalResistanceMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcThermalTransmittanceMeasure) {
      return "" + ((IfcThermalTransmittanceMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcTimeStamp) {
      return "" + ((IfcTimeStamp) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcVolumetricFlowRateMeasure) {
      return "" + ((IfcVolumetricFlowRateMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcMassPerLengthMeasure) {
      return "" + ((IfcMassPerLengthMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcTemperatureGradientMeasure) {
      return "" + ((IfcTemperatureGradientMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcSoundPowerMeasure) {
      return "" + ((IfcSoundPowerMeasure) derivedMeasureValue).getWrappedValue();
    } else if (derivedMeasureValue instanceof IfcSoundPressureMeasure) {
      return "" + ((IfcSoundPressureMeasure) derivedMeasureValue).getWrappedValue();
    } else {
      log.info("Unknown IfcDerivedMeasureValue value : " + derivedMeasureValue.toString());
    }
    return null;
  }

  /**
   * See http://www.buildingsmart-tech.org/ifc/IFC2x3/TC1/html/ifcquantityresource/lexical/ifcphysicalquantity.htm
   * for list of different {@link IfcPhysicalQuantity} types.
   *
   * @param ifcPhysicalQuantity
   * @return The string value from different {@link IfcPhysicalQuantity} types.
   */
  public String getStringValueFromIfcPhysicalQuantity(IfcPhysicalQuantity ifcPhysicalQuantity) {
    if (ifcPhysicalQuantity instanceof IfcQuantityArea) {
      return  ((IfcQuantityArea) ifcPhysicalQuantity).getAreaValueAsString();
    } else if (ifcPhysicalQuantity instanceof IfcQuantityLength) {
      return  ((IfcQuantityLength) ifcPhysicalQuantity).getLengthValueAsString();
    } else if (ifcPhysicalQuantity instanceof IfcQuantityVolume) {
      return  ((IfcQuantityVolume) ifcPhysicalQuantity).getVolumeValueAsString();
    } else if (ifcPhysicalQuantity instanceof IfcQuantityCount) {
      return  ((IfcQuantityCount) ifcPhysicalQuantity).getCountValueAsString();
    } else if (ifcPhysicalQuantity instanceof IfcQuantityWeight) {
      return  ((IfcQuantityWeight) ifcPhysicalQuantity).getWeightValueAsString();
    } else if (ifcPhysicalQuantity instanceof IfcQuantityTime) {
      return  ((IfcQuantityTime) ifcPhysicalQuantity).getTimeValueAsString();
    } else {
      log.info("Unknown IfcPhysicalQuantity value : " + ifcPhysicalQuantity.getName() + " "
          + ifcPhysicalQuantity);
    }
    return null;
  }

  /**
   * @param classObject
   * @return The IFC object class name without the 'Impl' suffix.
   */
  public String stripClassName(Class<?> classObject) {
    String name = classObject.getSimpleName();
    int implIndex = name.lastIndexOf("Impl");
    return name.substring(0, implIndex < 0 ? name.length() : implIndex);
  }

  /**
   * @param model
   * @return The {@link SIPrefix} that is applied to the IFC model.
   */
  public SIPrefix getLengthUnitPrefix(IfcModelInterface model) {
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
