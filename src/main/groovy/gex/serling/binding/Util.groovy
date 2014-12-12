package gex.serling.binding

import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.runtime.InvokerHelper
import org.springframework.stereotype.Component
import org.springframework.util.ReflectionUtils

import java.lang.reflect.Field

@Component
class Util {

  def static avoidList = ['metaClass', 'class']

  def static toDTO(Object source, Object dto, List<String> avoid = []) {
    def invalidFields = avoidList + avoid

    Map entities = [:]

    dto.properties.keySet().each {
      def entity = StringUtils.splitByCharacterTypeCamelCase(it)
      if (entity.length == 2) {
        entities.put(entity[0].toLowerCase(), entity[1].toLowerCase())
      } else if (entity.length == 3) {
        entities.put(entity[0].toLowerCase(), entity[1].toLowerCase() + entity[2].capitalize())
      }
    }

    def props = source.properties.findAll {
      (
        (
          it.key in dto.properties.keySet() ||
            (
              it.key in entities.keySet() &&
                (
                  DomainClassArtefactHandler.isDomainClass(source.getProperty(it.key).getClass()) ||
                    source.getProperty(it.key).getClass().isEnum()
                )
            )
        )
          &&
          !invalidFields.contains(it.key)
      )
    }

    use(InvokerHelper) {
      props.each { attribute ->
        def prop = source.getProperty(attribute.key)
        def propName = attribute.key

        if (DomainClassArtefactHandler.isDomainClass(prop.getClass())) {
          if (attribute.key in entities.keySet()) {
            dto.setProperty(attribute.key + entities.getAt(attribute.key).toString().capitalize(), prop.getProperty(entities.getAt(attribute.key)))
          } else {
            Field sourceField = ReflectionUtils.findField(dto.getClass(), propName)
            def destinationClass = sourceField.getGenericType()
            dto.setProperty(attribute.key, toDTO(prop, destinationClass))
          }
        } else if (prop.getClass().isEnum()) {
          if (attribute.key in entities.keySet()) {
            dto.setProperty(attribute.key + entities.getAt(attribute.key).toString().capitalize(), dto?."${entities.getAt(attribute.key)}")
          } else {
            Field sourceField = ReflectionUtils.findField(dto.getClass(), propName)
            def destinationClass = sourceField.getGenericType()
            dto.setProperty(attribute.key, toDTO(prop, destinationClass))
          }
        } else if (prop instanceof Collection) {

          def destList = []

          def listItem = prop.find { it != null }

          if (listItem) {

            Field sourceField = ReflectionUtils.findField(dto.getClass(), propName)
            def destinationClass
            if (sourceField?.getGenericType()?.actualTypeArguments?.length > 0) {
              destinationClass = sourceField?.getGenericType()?.actualTypeArguments[0]
            }

            if (destinationClass) {
              prop.each { theItem ->
                if (theItem != null) {
                  destList << toDTO(theItem, destinationClass)
                }
              }
            }

          }
          if (destList) {
            dto.setProperty(attribute.key, destList)
          }
        } else {
          if (attribute.value) {
            dto.setProperty(attribute.key, attribute.value)
          }
        }
      }
    }

    dto
  }

  def static toDTO(Object source, Class target, List<String> avoid = []) {
    def invalidFields = avoidList + avoid
    def dto = target.newInstance()

    toDTO(source, dto, invalidFields)
  }
}
