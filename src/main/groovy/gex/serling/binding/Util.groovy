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

    Map entities = [:].withDefault { [] }

    dto.properties.keySet().each {
      def entity = StringUtils.splitByCharacterTypeCamelCase(it)

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

    def props = source.properties.findAll {
      it.key in dto.properties.keySet() && !invalidFields.contains(it.key)
    }

    props += entities

    use(InvokerHelper) {
      props.each { attribute ->
        def prop = source.getProperty(attribute.key)
        def propName = attribute.key

        if (DomainClassArtefactHandler.isDomainClass(prop.getClass())) {
          if (entities[attribute.key]) {
            entities[attribute.key].each { attr ->
              dto.setProperty(attribute.key + attr.toString().capitalize(), prop.getProperty(attr))
            }
          } else {
            Field sourceField = ReflectionUtils.findField(dto.getClass(), propName)
            def destinationClass = sourceField.getGenericType()
            dto.setProperty(attribute.key, toDTO(prop, destinationClass))
          }
        } else if (prop.getClass().isEnum()) {
          if (entities[attribute.key]) {
            entities[attribute.key].each { attr ->
              dto.setProperty(attribute.key + attr.toString().capitalize(), dto?."${attr}")
            }
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

  private static boolean checkForEntity(Object source, String val) {
    source.hasProperty(val) && (DomainClassArtefactHandler.isDomainClass(source.getProperty(val).getClass()) ||
      source.getProperty(val).getClass().isEnum())
  }

  def static toDTO(Object source, Class target, List<String> avoid = []) {
    def invalidFields = avoidList + avoid
    def dto = target.newInstance()

    toDTO(source, dto, invalidFields)
  }
}
