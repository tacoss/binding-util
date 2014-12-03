package gex.serling.binding

import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.runtime.InvokerHelper
import org.springframework.stereotype.Component
import org.springframework.util.ReflectionUtils

import java.lang.reflect.Field

@Component
class Util {

  static List avoidList = ['metaClass', 'class']

  static Object bind(Object source, Object destination, List<String> avoid = []) {

    if(source == null){
      return null
    }

    def invalidFields = avoidList + avoid

    Map entities = [:]

    destination.properties.keySet().each {
      def entity = StringUtils.splitByCharacterTypeCamelCase(it)
      if (entity.length == 2) {
        entities.put(entity[0].toLowerCase(), entity[1].toLowerCase())
      } else if (entity.length == 3) {
        entities.put(entity[0].toLowerCase(), entity[1].toLowerCase() + entity[2].capitalize())
      }
    }

    def props = source.properties.findAll {
      (((it.key in destination.properties.keySet()) || (it.key in entities.keySet() && DomainClassArtefactHandler.isDomainClass(source.getProperty(it.key).getClass()))) &&
        !invalidFields.contains(it.key))
    }

    use(InvokerHelper) {
      props.each { attribute ->
        def prop = source.getProperty(attribute.key)
        def propName = attribute.key

        if (DomainClassArtefactHandler.isDomainClass(prop.getClass())) {
          if (attribute.key in entities.keySet()) {
            destination.setProperty(attribute.key + entities.getAt(attribute.key).toString().capitalize(), prop.getProperty(entities.getAt(attribute.key)))
          } else {
            Field sourceField = ReflectionUtils.findField(destination.getClass(), propName)
            if (sourceField.getGenericType()?.actualTypeArguments?.length > 0) {
              def destinationClass = sourceField.getGenericType()?.actualTypeArguments[0]
              if (destinationClass) {
                destination.setProperty(attribute.key, bind(attribute.key, destinationClass))
              }
            }
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
          if (attribute.value) {
            destination.setProperty(attribute.key, attribute.value)
          }
        }
      }
    }

    destination
  }

  static Object bind(Object source, Class target, List<String> avoid = []) {
    def invalidFields = avoidList + avoid
    def dto = target.newInstance()

    bind(source, dto, invalidFields)
  }
}
