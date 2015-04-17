package gex.serling.binding

import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.runtime.InvokerHelper
import org.springframework.stereotype.Component
import org.springframework.util.ReflectionUtils

import java.lang.reflect.Array
import java.lang.reflect.Field

@Component
class Util {

  def static tache = [String, Integer, Array, List, Boolean, Collections]

  static List avoidList = ['metaClass', 'class']
  
  List<DynamicMapping> dynamicBindings
  
  Set<String> exclusions

  Boolean bindNullValues
  
  Util(){
    dynamicBindings = []
    exclusions = []
    bindNullValues = false
  }

  Boolean getBindNullValues() {
    return bindNullValues
  }

  void setBindNullValues(Boolean bindNullValues) {
    this.bindNullValues = bindNullValues
  }


  void registerBinding(DynamicMapping bindingEntry){
    dynamicBindings = dynamicBindings ?: []
    bindingEntry.customBindings ?: [:]

    bindingEntry.exclusions.each {
      bindingEntry.customBindings.put( it, { x -> null })
    }
    
    dynamicBindings.add(bindingEntry)
  }

  
  def static bind(Object source, Class target, List<String> avoid = [], Boolean bindNullValues = true) {
    def invalidFields = avoidList + avoid
    def dto = target.newInstance()
    bind(source, dto, invalidFields, bindNullValues)
  }

  def static bind(Object source, Object destination, List<String> avoid = [], Boolean bindNullValues = true) {
    new Util(exclusions: avoid, dynamicBindings: [], bindNullValues: bindNullValues).dynamicBind(source, destination)
  }

  def dynamicBind(Object source, Class target, Map extraParams = null) {
    def dto = target.newInstance()
    dynamicBind(source, dto, extraParams)
  }

  def dynamicBind(Object source, Object destination, Map extraParams = null){
    
    if (source instanceof Class){
      throw new IllegalArgumentException("Source object must be an instance, not a class")
    }
    
    if(source == null){
      return null
    }

    def invalidFields = avoidList + exclusions
    
    def destinationProperties = getValidDestinationProperties(destination, invalidFields, source)
    
    Set<String> destinationsProps = destinationProperties.destinationsProps
    Map entities = destinationProperties.entities
    
    def props = [:]

    getCustomBindings(source, destination)?.keySet().each { k ->
      props.put(k, source)
    }
    
    props += getSourceProperties(source, destinationsProps)
    props += entities

    use(InvokerHelper) {
      props.each { attribute ->
        
        def dynamicBinding = getDynamicBindingValue(source, destination, extraParams, attribute)

        if(dynamicBinding && dynamicBinding.existDynamicBinding){
          if( destination.properties.containsKey(attribute.key)) {
            destination.setProperty(attribute.key, dynamicBinding.value)
          }
        }else {

          def prop = source.getProperty(attribute.key)

          if (   !tache.contains(obj.getClass() /*DomainClassArtefactHandler.isDomainClass(prop.getClass())*/)) {
            processDomainClass(source, destination, attribute, entities)
          } else if (prop.getClass().isEnum()) {
            processEnum(source, destination, attribute, entities)
          } else if (prop instanceof Collection) {
            processCollection(source, destination, attribute, entities)
          } else {
            processSimpleProperty(source, destination, attribute, entities)
          }
        }
      }
    }
    
    destination
  }
  
  def processDomainClass(Object source,  Object destination,  def attribute, Map entities){
    def prop = source.getProperty(attribute.key)
//    def propName = attribute.key
    
//    if (entities[attribute.key]) {
      entities[attribute.key].each { attr ->
        destination.setProperty(attribute.key + attr.toString().capitalize(), prop.getProperty(attr))
      }
//    } else {
//      Field sourceField = ReflectionUtils.findField(destination.getClass(), propName)
//      def destinationClass = sourceField.getGenericType()
//      destination.setProperty(attribute.key, bind(prop, destinationClass))
//    }
  }
  
  
  def processEnum(Object source,  Object destination,  def attribute, Map entities){
    def prop = source.getProperty(attribute.key)
    def propName = attribute.key
    
    if (entities[attribute.key]) {
      entities[attribute.key].each { attr ->
        destination.setProperty(attribute.key + attr.toString().capitalize(), prop?."${attr}")
      }
    } else {
      Field sourceField = ReflectionUtils.findField(destination.getClass(), propName)
      def destinationClass = sourceField.getGenericType()
      destination.setProperty(attribute.key, bind(prop, destinationClass))
    }
  }
  
  
  def processCollection(Object source,  Object destination,  def attribute, Map entities){
    def destList = []
    def prop = source.getProperty(attribute.key)
    def propName = attribute.key

    def listItem = prop.find { it != null }

    if (listItem) {

      Field sourceField = ReflectionUtils.findField(destination.getClass(), propName)
      def destinationClass

      // When List is not typed, we make and straight bind
      if(!sourceField?.getGenericType().properties.containsKey('actualTypeArguments')) {
        destList = prop
      }
      else if(sourceField?.getGenericType()?.actualTypeArguments?.length > 0) {
        destinationClass = sourceField?.getGenericType()?.actualTypeArguments[0]
      }

      if (destinationClass) {
        prop.each { theItem ->
          if (theItem != null) {
            destList << bind(theItem, destinationClass)
          }
        }
      }

    }
    if (destList != null) {
      destination.setProperty(attribute.key, destList)
    }
  }

  def processSimpleProperty(Object source,  Object destination,  def attribute, Map entities){
    if (attribute.value != null ||  bindNullValues == true) {
      destination.setProperty(attribute.key, attribute.value)
    }
  }
  
  def Map getDynamicBindingValue(Object source,  Object destination,  Map extraParams, def attribute){
    Map result
    
    if(dynamicBindings){
      Closure customClosure = getCustomBindings(source, destination)?.get(attribute.key)
      
      if(customClosure != null){
        result = [existDynamicBinding: true]
        
        if(customClosure.maximumNumberOfParameters == 1){
          //e.g., {val -> val}
          result.value = customClosure.call(attribute.value)
        } else if(customClosure.maximumNumberOfParameters == 2){
          //e.g.,  {val, obj -> val, obj}
          result.value = customClosure.call(attribute.value, source)
        }
        else if(customClosure.maximumNumberOfParameters == 3){
          //e.g.,  {val, obj, extraParams -> val, obj, extraParams}
          result.value = customClosure.call(attribute.value, source, extraParams)
        }
      }
    }
    
    result
  }


  Map<String, Closure> getCustomBindings(def source, def destination){
    def result

    if(dynamicBindings) {
      result = dynamicBindings.find {
        it.sourceClass.name == source.class.name &&
        it.destinationClass.name == destination.class.name
      }?.customBindings
    }

    result
  }

  def static getSourceProperties(Object source, Set<String> destinationsProps){
    Map properties = [:]

    source.getClass().getDeclaredFields().findAll{
      !it.isSynthetic()
    }.each{
      properties.put(it.name, source."${it.name}")
    }

    properties.findAll { it.key in  destinationsProps}
  }

  private Map getValidDestinationProperties(Object destination, List invalidFields, Object source) {
    Set<String> destinationsProps = destination.properties.keySet().findAll { !(it in invalidFields) }

    Map entities = [:].withDefault { [] }

    destinationsProps.each {
      String[] entity = StringUtils.splitByCharacterTypeCamelCase(it)

      if (entity.length > 0) {
        def entitiName = entity.inject("") {

          acc, val ->
          if (checkForEntity(source, acc + val)) {
            acc + val
          }
          else
            if (checkForEntity(source, acc)) {
            String value = it.split("$acc")[1]
            value = value.replaceFirst(value[0], value[0].toLowerCase())
            entities."$acc" << value
            acc
          }
          else {
            acc = acc + val
            acc
          }

        }
      }
//      else {
//        if (checkForEntity(it)) {
//          entities."$it"
//        }
//      }
    }

    def result = [
      destinationsProps: destinationsProps,
      entities: entities
    ]
    result
  }
  
  private static boolean checkForEntity(Object source, String val) {

    def es = source.hasProperty(val)

    if(es ){
      def v = source.getProperty(val)
      es =  x(v) ||  source.getProperty(val).getClass().isEnum()
    }


//    if(source.hasProperty(val) &&
//      (DomainClassArtefactHandler.isDomainClass(source.getProperty(val).getClass())))
//    {
//      println "Class ${source.getProperty(val).getClass()}"
//      println "...>" +  DomainClassArtefactHandler.isDomainClass(source.getProperty(val).getClass())
//    }


    //source.getClass()
    es
  }


  def static x(Object obj){
    def result = false


    if( !tache.contains(obj.getClass())  ){
      result =  true
    }
    result
  }


}

