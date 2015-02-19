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

  List<BindingEntry> binding
  
  static Util auxForStatic = new Util( binding: [] )

  void registerBinding(BindingEntry bindingEntry){
    binding = binding ?: []
    binding.add(bindingEntry)
  }

  static List avoidList = ['metaClass', 'class']

  
  
  def superBind(Object source, Object destination, List<String> avoid = [], List<BindingEntry> customBinding = []){
    
    if(source == null){
      return null
    }

    def invalidFields = avoidList + avoid

    Map entities = [:].withDefault { [] }
    Set<String> destinationsProps = destination.properties.keySet().findAll { !(it in invalidFields) }


    
    
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

    def props = getSourceProperties(source, destinationsProps)
    props += entities

    use(InvokerHelper) {
      props.each { attribute ->
        def prop = source.getProperty(attribute.key)
        def propName = attribute.key

        if (DomainClassArtefactHandler.isDomainClass(prop.getClass())) {
          if (entities[attribute.key]) {
            entities[attribute.key].each { attr ->
              destination.setProperty(attribute.key + attr.toString().capitalize(), prop.getProperty(attr))
            }
          } else {
            Field sourceField = ReflectionUtils.findField(destination.getClass(), propName)
            def destinationClass = sourceField.getGenericType()
            destination.setProperty(attribute.key, bind(prop, destinationClass))
          }
        } else if (prop.getClass().isEnum()) {
          if (entities[attribute.key]) {
            entities[attribute.key].each { attr ->
              destination.setProperty(attribute.key + attr.toString().capitalize(), destination?."${attr}")
            }
          } else {
            Field sourceField = ReflectionUtils.findField(destination.getClass(), propName)
            def destinationClass = sourceField.getGenericType()
            destination.setProperty(attribute.key, bind(prop, destinationClass))
          }
        } else if (prop instanceof Collection) {

          def destList = []

          def listItem = prop.find { it != null }

          if (listItem) {

            Field sourceField = ReflectionUtils.findField(destination.getClass(), propName)
            def destinationClass
            if (sourceField?.getGenericType()?.actualTypeArguments?.length > 0) {
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
          if (destList) {
            destination.setProperty(attribute.key, destList)
          }
        } else {
          if (attribute.value != null) {
            destination.setProperty(attribute.key, attribute.value)

            if(binding){
              def x = binding.find{
                it.source.name == source.class.name
                it.destination.name == destination.class.name
              }

              def funct = x.customBinding.get(attribute.key)

              if(funct) {
                destination.setProperty(attribute.key, funct(attribute.value))
              }
            }
          }
        }
      }
    }


    
    

    destination
    
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

  def static bind(Object source, Object destination, List<String> avoid = []) {
    auxForStatic.superBind(source, destination, avoid)
  }

  private static boolean checkForEntity(Object source, String val) {
    source.hasProperty(val) && (DomainClassArtefactHandler.isDomainClass(source.getProperty(val).getClass()) ||
      source.getProperty(val).getClass().isEnum())
  }

  def static bind(Object source, Class target, List<String> avoid = []) {
    def invalidFields = avoidList + avoid
    def dto = target.newInstance()

    bind(source, dto, invalidFields)
  }

  def superBind(Object source, Class target, List<String> avoid = []) {
    def dto = target.newInstance()
    superBind(source, dto)
  }
}

