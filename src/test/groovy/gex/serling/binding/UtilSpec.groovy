package gex.serling.binding

import gex.serling.binding.domain.Enemy
import gex.serling.binding.domain.Superpower
import gex.serling.binding.dto.Hero
import spock.lang.Ignore
import spock.lang.IgnoreRest
import spock.lang.Specification
import org.springframework.boot.test.IntegrationTest
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Unroll

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
      def object = util.bind(new Hero(name: 'name'), Hero)
    then:
      object.name == 'name'
  }

  def 'Bind between an entity object and a pojo is made without problem'(){
    given:
      gex.serling.binding.domain.Hero domain = new gex.serling.binding.domain.Hero(name: 'Dalek Caan')
      println domain
      domain.save(flush: true)
      println domain.properties

    when:
      Hero dto = Util.bind(domain, Hero.class)
      println dto.properties

    then:
      dto.name == domain.name
  }

  def 'it is binding all properties except the explicit avoided ones'(){
    given:
      Hero dto = new Hero()
      dto.name = "The doctor"
      dto.age = 904
      dto.isInmortal = true
      dto.otherNames = ['Jonh Smith', 'Doctor who?']

    when:
      gex.serling.binding.domain.Hero domain = Util.bind(dto, gex.serling.binding.domain.Hero.class, ['notPersistedField', 'isInmortal'])

    then:
      domain.name == dto.name
      domain.age == dto.age
      domain.isInmortal == null
  }

  def 'It binds embeddedObjects using camelCase'(){
    given:
      gex.serling.binding.domain.Hero domainHero = new gex.serling.binding.domain.Hero()
      domainHero.name = "The doctor"
      domainHero.age = 904
      domainHero.isInmortal = true
      domainHero.superpower = new Superpower(name: 'Regeneration')

    when:
      Hero dtoHero = Util.bind(domainHero, Hero.class)

    then:
      dtoHero.name == domainHero.name
      dtoHero.superpowerName == domainHero.superpower.name
  }

  def 'It binds also properties that are lists'(){
    given:
      gex.serling.binding.domain.Hero domainHero = new gex.serling.binding.domain.Hero()
      domainHero.name = "The doctor"
      domainHero.enemies = [new Enemy(name: 'Dalek'), new Enemy(name: 'Cyberman'), new Enemy(name: 'Weeping Angel')]

    when:
      Hero dtoHero = Util.bind(domainHero, Hero.class)

    then:
      dtoHero.name == domainHero.name
      dtoHero.enemies.each{
        println(it)
        domainHero.enemies*.name.contains(it.name)
      }
  }

  @Unroll
  def 'It correctly binds  booleans >> When #booleanValue'() {
    given:
      gex.serling.binding.domain.Hero domainHero = new gex.serling.binding.domain.Hero()
      domainHero.name = "The Doctor"
      domainHero.isInmortal = booleanValue

    when:
      Hero dtoHero = Util.bind(domainHero, Hero.class)

    then:
      dtoHero.name == domainHero.name
      dtoHero.isInmortal == bindedBooleanValue

    where:
      booleanValue || bindedBooleanValue
      null         || null
      false        || false
      true         || true
  }

}
