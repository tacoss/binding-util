package gex.serling.binding

import org.springframework.boot.test.IntegrationTest
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification


/**
 * Created by Tsunllly on 1/26/15.
 */

@IntegrationTest
@ContextConfiguration(loader = SpringApplicationContextLoader, classes = TestApplication)
@Transactional
class UtilBindSpec extends Specification{


  def 'test'(){
    given:
      ExampleDomain domain = new ExampleDomain(name: 'Cloud')
      println domain
      domain.save(flush: true)
      println domain.properties
      println "...."

    when:
      ExampleDto dto = Util.bind(domain, ExampleDto.class)
      println dto.properties

    then:
      dto.name == domain.name
  }
}
