package gex.serling.binding

import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.runtime.InvokerHelper
import org.springframework.stereotype.Component
import org.springframework.util.ReflectionUtils

import java.lang.reflect.Field
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass

@Component
class Util {

  static List avoidList = ['metaClass', 'class']
  
  List<BindingEntry> dynamicBindings
  List<String> exclusions
  
  
  Util(){
    dynamicBindings = []
    exclusions = []
  }

  Util(List<BindingEntry> dynamicBindings, List<String> exclusions) {
    this.dynamicBindings = dynamicBindings
    this.exclusions = exclusions
  }

  void registerBinding(BindingEntry bindingEntry){
    dynamicBindings = dynamicBindings ?: []
    dynamicBindings.add(bindingEntry)
  }

  void registerExclusion(String exclusion){
    exclusions = exclusions ?: []
    exclusions.add(exclusion)
  }

  def static bind(Object source, Class target, List<String> avoid = []) {
    def invalidFields = avoidList + avoid
    def dto = target.newInstance()
    bind(source, dto, invalidFields)
  }

  def static bind(Object source, Object destination, List<String> avoid = []) {
    new Util(exclusions: avoid, dynamicBindings: []).dynamicBind(source, destination)
  }

  def dynamicBind(Object source, Class target) {
    def dto = target.newInstance()
    dynamicBind(source, dto)
  }

  def dynamicBind(Object source, Object destination){
    if(source == null){
      return null
    }

    def invalidFields = avoidList + exclusions
    def destinationProperties = getValidDestinationProperties(destination, invalidFields, source)
    
    Set<String> destinationsProps = destinationProperties.destinationsProps
    Map destinationEntities = destinationProperties.entities

    def sourceProperties = getSourceProperties(source, destinationsProps)
    sourceProperties += destinationEntities

    use(InvokerHelper) {
      sourceProperties.each { sourcePropertyEntry ->

        def result
        def sourceProperty = source.getProperty(sourcePropertyEntry.key)

        if (DomainClassArtefactHandler.isDomainClass(sourceProperty.getClass())) {
          result = processDomainClass(source, destination, sourcePropertyEntry, destinationEntities)
        } else if (sourceProperty.getClass().isEnum()) {
          result = processEnum(source, destination, sourcePropertyEntry, destinationEntities)
        } else if (sourceProperty instanceof Collection) {
          result = processCollection(source, destination, sourcePropertyEntry, destinationEntities)
        } else {
          result = processSimpleProperty(source, destination, sourcePropertyEntry, destinationEntities)
        }

        if(dynamicBindings){
          result = processDynamicBinding(source, destination, sourcePropertyEntry, destinationEntities) ?: result
        }
        
        if(result) {
          destination.setProperty(result.key, result.value)
        }
        
      }
    }
    
    destination
  }
  
  def Map processDomainClass(Object source,  Object destination,  def sourcePropertyEntry, Map destinationEntities){
    String key
    String value
    
    String sourcePropName = sourcePropertyEntry.key
    def sourceProperty = source.getProperty(sourcePropName)

    if (destinationEntities[sourcePropName]) {
      destinationEntities[sourcePropName].each { attr ->
        key = sourcePropName + attr.toString().capitalize()
        value = sourceProperty.getProperty(attr)
      }
    } else {
      Field sourceField = ReflectionUtils.findField(destination.getClass(), sourcePropName)
      def destinationClass = sourceField.getGenericType()
      key = sourcePropertyEntry.name
      value = dynamicBind(sourceProperty, destinationClass)
    }
    
    [key: key, value: value] 
  }
  
  
  def processEnum(Object source,  Object destination,  def sourcePropertyEntry, Map destinationEntities){
    String key
    String value

    String sourcePropName = sourcePropertyEntry.key
    def sourceProperty = source.getProperty(sourcePropName)


    if (destinationEntities[sourcePropName]) {
      destinationEntities[sourcePropName].each { attr ->
        key = sourcePropName + attr.toString().capitalize()
        value = destination?."${attr}"
      }
    } else {
      Field sourceField = ReflectionUtils.findField(destination.getClass(), sourcePropName)
      def destinationClass = sourceField.getGenericType()
      key = sourcePropName
      value = dynamicBind(sourceProperty, destinationClass)
    }

    [key: key, value: value]
  }
  
  
  def processCollection(Object source,  Object destination,  def sourcePropertyEntry, Map destinationEntities){
    Map destinationValue

    String sourcePropName = sourcePropertyEntry.key
    def sourceProperty = source.getProperty(sourcePropName)
    
    def destList = []

    def listItem = sourceProperty.find { it != null }

    if (listItem) {

      Field sourceField = ReflectionUtils.findField(destination.getClass(), sourcePropName)
      def destinationClass
      if (sourceField?.getGenericType()?.actualTypeArguments?.length > 0) {
        destinationClass = sourceField?.getGenericType()?.actualTypeArguments[0]
      }

      if (destinationClass) {
        sourceProperty.each { theItem ->
          if (theItem != null) {
            destList << dynamicBind(theItem, destinationClass)
          }
        }
      }

    }
    if (destList) {
      destinationValue = [
        key: sourcePropName,
        value: destList
      ]
    }
    destinationValue
  }

  
  def processSimpleProperty(Object source,  Object destination,  def sourcePropertyEntry, Map destinationEntities){
    Map destinationValue
    
    if (sourcePropertyEntry.value != null) {
      destinationValue = [
        key: sourcePropertyEntry.key,
        value:  sourcePropertyEntry.value
      ]
    }
    
    destinationValue
  }

  def processDynamicBinding(Object source,  Object destination,  def sourcePropertyEntry, Map destinationEntities){
    Map destinationValue

    def customClosure = dynamicBindings.find{
      it.source.name == source.class.name
      it.destination.name == destination.class.name
    }?.customBindings?.get(sourcePropertyEntry.key)
    
    
    if(customClosure) {
      destinationValue = [
        key: sourcePropertyEntry.key,
        value: customClosure(sourcePropertyEntry.value)
      ]
    }
    
    destinationValue
  }

  def static getSourceProperties(Object source, Set<String> destinationsProps){
    Map<String, Object> props = [:]
    if(DomainClassArtefactHandler.isDomainClass(source.class)) {
      def d = new DefaultGrailsDomainClass(source.class)
      props = ( d.properties.toList() << [ name:'id' ]).collectEntries {
        [ ( it.name ): source."${it.name}" ]
      }
    } else {
      props  = source.properties
    }
    props.findAll { it.key in  destinationsProps}
  }

  private Map getValidDestinationProperties(Object destination, List invalidFields, Object source) {
    Set<String> destinationsProps = destination.properties.keySet().findAll { !(it in invalidFields) }

    Map entities = [:].withDefault { [] }

    destinationsProps.each {
      String[] entity = StringUtils.splitByCharacterTypeCamelCase(it)

      if (entity.length > 0) {
        def entitiName = entity.inject("") { acc, val ->
          if (checkForEntity(source, acc + val)) {
            acc + val
          } else if (checkForEntity(source, acc)) {
            String value = it.split("$acc")[1]
            value = value.replaceFirst(value[0], value[0].toLowerCase())
            entities."$acc" << value
            acc
          } else {
            acc = acc + val
            acc
          }
        }
      } else {
        if (checkForEntity(it)) {
          entities."$it"
        }
      }
    }

    def result = [
      destinationsProps: destinationsProps,
      entities: entities
    ]
    result
  }
  
  private static boolean checkForEntity(Object source, String val) {
    source.hasProperty(val) && (DomainClassArtefactHandler.isDomainClass(source.getProperty(val).getClass()) ||
      source.getProperty(val).getClass().isEnum())
  }
  
}

