package gex.serling.binding

import gex.serling.binding.domain.Enemy
import gex.serling.binding.domain.Status
import gex.serling.binding.domain.Superpower
import gex.serling.binding.dto.Hero
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


  def 'It binds enums properties correctly'(){
    given:
      gex.serling.binding.domain.Hero domainHero = new gex.serling.binding.domain.Hero()
      domainHero.name = "The doctor"
      domainHero.age = 904
      domainHero.isInmortal = true
      domainHero.superpower = new Superpower(name: 'Regeneration')
      domainHero.status = Status.DELETED

    when:
      Hero dtoHero = Util.bind(domainHero, Hero.class)

    then:
      dtoHero.name == domainHero.name
      dtoHero.superpowerName == domainHero.superpower.name
      dtoHero.statusId == Status.DELETED.id
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
      dtoHero.enemies*.name.containsAll(domainHero.enemies*.name)
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

  def 'It binds CamelCase properties, no matter they are not embedded objects'(){
    given:
      gex.serling.binding.domain.Hero domainHero = new gex.serling.binding.domain.Hero()
      domainHero.name = "The doctor"
      domainHero.enemies = [new Enemy(name: 'Dalek'), new Enemy(name: 'Cyberman'), new Enemy(name: 'Weeping Angel')]

    when:
      Hero dtoHero = Util.bind(domainHero, Hero.class)

    then:
      dtoHero.name == domainHero.name
      dtoHero.enemies.each{
        domainHero.enemies*.name.contains(it.name)
      }
  }

  def 'It can be specified a dynamic way to bind properties (simple properties)'(){
    when:
      def util = new Util()

      Map map = [
        "age" : { x -> x * 10 }
      ]

      def cb = new DynamicBinding(source: Hero.class, destination: gex.serling.binding.domain.Hero.class, customBindings: map )

      util.registerBinding( cb )

      def object = util.dynamicBind(new Hero(name: 'Goku', age: 21 ), gex.serling.binding.domain.Hero)
    then:

      object.name == 'Goku'
      object.age == 210
  }

  def 'It can be specified a dynamic way to bind properties (collection properties)'(){
    given:
      def util = new Util()

      def hardcodedEnemies = [new Enemy(name: 'OtroDale'), new Enemy(name: 'OtroCyberman'), new Enemy(name: 'Otro Weeping Ange')]
      Map map = [
        "enemies" : { x -> hardcodedEnemies }
      ]

      def cb = new DynamicBinding(source: gex.serling.binding.domain.Hero.class, destination: Hero.class , customBindings: map )

      util.registerBinding( cb )

      gex.serling.binding.domain.Hero domainHero = new gex.serling.binding.domain.Hero()
      domainHero.name = "The doctor"
      domainHero.enemies = [new Enemy(name: 'Dalek'), new Enemy(name: 'Cyberman'), new Enemy(name: 'Weeping Angel')]

    when:
      Hero dtoHero = util.dynamicBind(domainHero, Hero.class)

    then:
      dtoHero.name == domainHero.name
      dtoHero.enemies.containsAll(hardcodedEnemies)
  }
  
  def 'A configured instance is used for multiple bindings'(){
    given:
      def util = new Util()
    
      // Register bindings
      def hardcodedEnemies = [new Enemy(name: 'Silence'), new Enemy(name: 'Dark')]
      Map mappings = [
        "age" : { x -> x * 10 },
        "enemies" : { x -> hardcodedEnemies }
      ]
      def db = new DynamicBinding(source: gex.serling.binding.domain.Hero.class, destination: Hero.class, customBindings: mappings )
      util.registerBinding( db )
      
      // Register exclusions
      util.registerExclusions(['notPersistedField', 'isInmortal'])
    
    when: 'A binding'

      gex.serling.binding.domain.Hero domainHero = new gex.serling.binding.domain.Hero()
      domainHero.name = "The doctor"
      domainHero.enemies = [new Enemy(name: 'Dalek'), new Enemy(name: 'Cyberman'), new Enemy(name: 'Weeping Angel')]
      domainHero.age = 94
      domainHero.isInmortal = true
      domainHero.status = Status.ACTIVE
    
      Hero dtoHero = util.dynamicBind(domainHero, Hero.class)

    then:
      dtoHero.name == "The doctor"
      dtoHero.enemies.containsAll(hardcodedEnemies)
      dtoHero.age == 940
      dtoHero.statusId == Status.ACTIVE.id
      dtoHero.isInmortal == null
      dtoHero.notPersistedField == null

    when: 'A second binding'

      domainHero = new gex.serling.binding.domain.Hero()
      domainHero.name = "Pikachu"
      domainHero.enemies = [new Enemy(name: 'Jessy'), new Enemy(name: 'James')]
      domainHero.age = 5
      domainHero.isInmortal = false
      domainHero.status = Status.SUSPENDED

      dtoHero = util.dynamicBind(domainHero, Hero.class)

    then:
      dtoHero.name == "Pikachu"
      dtoHero.enemies.containsAll(hardcodedEnemies)
      dtoHero.age == 50
      dtoHero.statusId == Status.SUSPENDED.id
      dtoHero.isInmortal == null
      dtoHero.notPersistedField == null
  }
  
}
