package gex.serling.binding

import spock.lang.IgnoreRest
import spock.lang.Specification
import org.springframework.boot.test.IntegrationTest
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional

/**
 * Created by domix on 12/26/14.
 */
@IntegrationTest
@ContextConfiguration(loader = SpringApplicationContextLoader, classes = TestApplication)
@Transactional
class UtilSpec extends Specification {


  def 'should bind a new Instance taking a instanciated object'() {
    when:
      def util = new Util()
      def object = util.bind(new Demo(name: 'name'), Demo)
    then:
      object.name == 'name'
  }

  @IgnoreRest
  def 'test'(){
    given:
      ExampleDomain domain = new ExampleDomain(name: 'Cloud')
      println domain
      domain.save(flush: true)
      println domain.properties

    when:
      ExampleDto dto = Util.bind(domain, ExampleDto.class)
      println dto.properties

    then:
      dto.name == domain.name
  }




}

class Demo {
  String name
}