package gex.serling.binding

import gex.serling.binding.domain.Enemy as DomainEnemy
import gex.serling.binding.domain.Planet
import gex.serling.binding.domain.Status
import gex.serling.binding.domain.Superpower as DomainSuperpower
import gex.serling.binding.domain.Hero as DomainHero
import gex.serling.binding.dto.Enemy
import gex.serling.binding.dto.Hero
import gex.serling.binding.dto.Superpower
import spock.lang.Issue
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
      def object = Util.bind(new Hero(name: 'The Doctor'), Hero)
    then:
      object.name == 'The Doctor'
  }
  
  def 'Bind between an entity object and a pojo is made without problem'(){
    given:
      gex.serling.binding.domain.Hero domain = new gex.serling.binding.domain.Hero(name: 'Dalek Caan')
      println domain
      domain.save(flush: true)

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
      domainHero.superpower = new DomainSuperpower(name: 'Regeneration')

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
      domainHero.superpower = new DomainSuperpower(name: 'Regeneration')
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
      domainHero.enemies = [new DomainEnemy(name: 'Dalek'), new DomainEnemy(name: 'Cyberman'), new DomainEnemy(name: 'Weeping Angel')]

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
      domainHero.enemies = [new DomainEnemy(name: 'Dalek'), new DomainEnemy(name: 'Cyberman'), new DomainEnemy(name: 'Weeping Angel')]

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

      def cb = new DynamicMapping(sourceClass: Hero.class, destinationClass: gex.serling.binding.domain.Hero.class, customBindings: map )

      util.registerBinding( cb )

      def object = util.dynamicBind(new Hero(name: 'Goku', age: 21 ), gex.serling.binding.domain.Hero)
    then:

      object.name == 'Goku'
      object.age == 210
  }

  def 'It can be specified a dynamic way to bind properties (collection properties)'(){
    given:
      def util = new Util()

      def hardcodedEnemies = [new DomainEnemy(name: 'OtroDale'), new DomainEnemy(name: 'OtroCyberman'), new DomainEnemy(name: 'Otro Weeping Ange')]
      Map map = [
        "enemies" : { x -> hardcodedEnemies }
      ]

      def cb = new DynamicMapping(sourceClass: gex.serling.binding.domain.Hero.class, destinationClass: Hero.class , customBindings: map )

      util.registerBinding( cb )

      gex.serling.binding.domain.Hero domainHero = new gex.serling.binding.domain.Hero()
      domainHero.name = "The doctor"
      domainHero.enemies = [new DomainEnemy(name: 'Dalek'), new DomainEnemy(name: 'Cyberman'), new DomainEnemy(name: 'Weeping Angel')]

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
      def hardcodedEnemies = [new DomainEnemy(name: 'Silence'), new DomainEnemy(name: 'Dark')]

      Map mappings = [
        "age" : { x, y -> x * 10 },
        "enemies" : { x -> hardcodedEnemies },
        "separatedByCommaEnemies" : {val, obj -> obj.enemies*.name.join(",")},
        "lastName": {val, obj, extra ->  extra[obj.name] }
      ]

      def db = new DynamicMapping(
        sourceClass: gex.serling.binding.domain.Hero.class,
        destinationClass: Hero.class,
        customBindings: mappings,
        exclusions: ["notPersistedField", "isInmortal"])

      util.registerBinding( db )

      // Aux map
      Map extraParams = ['The doctor': 'Smith', 'Pikachu': 'Mon' ]

    when: 'A binding'
      gex.serling.binding.domain.Hero domainHero = new gex.serling.binding.domain.Hero()
      domainHero.name = "The doctor"
      domainHero.enemies = [new DomainEnemy(name: 'Dalek'), new DomainEnemy(name: 'Cyberman'), new DomainEnemy(name: 'Weeping Angel')]
      domainHero.age = 94
      domainHero.isInmortal = true
      domainHero.status = Status.ACTIVE
    
      Hero dtoHero = util.dynamicBind(domainHero, Hero, extraParams)

    then:
      dtoHero.name == "The doctor"
      dtoHero.lastName == 'Smith'
      dtoHero.enemies.containsAll(hardcodedEnemies)
      dtoHero.age == 940
      dtoHero.statusId == Status.ACTIVE.id
      dtoHero.isInmortal == null
      dtoHero.notPersistedField == null
      dtoHero.separatedByCommaEnemies == "Dalek,Cyberman,Weeping Angel"
      

    when: 'A second binding'
      domainHero = new gex.serling.binding.domain.Hero()
      domainHero.name = "Pikachu"
      dtoHero.lastName == 'Mon'
      domainHero.enemies = [new DomainEnemy(name: 'Jessy'), new DomainEnemy(name: 'James')]
      domainHero.age = 5
      domainHero.isInmortal = false
      domainHero.status = Status.SUSPENDED

      dtoHero = util.dynamicBind(domainHero, Hero.class, extraParams)

    then:
      dtoHero.name == "Pikachu"
      dtoHero.enemies.containsAll(hardcodedEnemies)
      dtoHero.age == 50
      dtoHero.statusId == Status.SUSPENDED.id
      dtoHero.isInmortal == null
      dtoHero.notPersistedField == null
      dtoHero.separatedByCommaEnemies == "Jessy,James"
  }

  @Issue(value = '3')
  def 'should bind correctly null values (default behaviour)'() {
    when:
      DomainHero destination = new DomainHero(
        name: "Superman",
        isInmortal: true,
        superpower: new DomainSuperpower(name: 'Fly'),
        enemies: [new DomainEnemy(name: 'Lex Luthor')],
        planet: Planet.KRYPTON
      )

      destination = Util.bind(new DomainHero(superpower: null, enemies: null, planet: null, isInmortal: isImmortal), destination)
    then:
      destination.name == null
      destination.isInmortal == expected
      destination.superpower == null
      destination.enemies == null
      destination.planet == null

    where:
      isImmortal || expected
      null       || null
      false      || false
  }

  @Issue(value = '3')
  def 'should not bind null values when property bindNullValues is set to false'() {
    given:
      DomainHero destination = new DomainHero(
        name: "Superman",
        isInmortal: true,
        superpower: new DomainSuperpower(name: 'Fly'),
        enemies: [new DomainEnemy(name: 'Lex Luthor')],
        planet: Planet.KRYPTON
      )

      def assertions = { dest->
        assert dest.name == 'Superman'
        assert dest.isInmortal == expected
        assert dest.superpower.name == 'Fly'
        assert dest.enemies[0].name == 'Lex Luthor'
        assert dest.planet == Planet.KRYPTON
        true
      }

    when: 'making static binding'
      destination = Util.bind(new DomainHero(superpower: null, enemies: null, planet: null, isInmortal: isImmortal), destination, [], false)

    then:
      assertions(destination)

    when: 'making dynamic binding'
      def util = new Util(bindNullValues: false)
      destination == util.dynamicBind(new DomainHero(superpower: null, enemies: null, planet: null, isInmortal: isImmortal), destination)

    then:
      assertions(destination)

    where:
      isImmortal || expected
      null       || true
      false      || false
  }

  @Issue(value = '20')
  def 'should bind correctly when no customBindings are defined but exclusions are'() {
    when:
      def util = new Util()
      def dm = new DynamicMapping(
        sourceClass: Hero,
        destinationClass: DomainHero,
        exclusions: ['age', 'otherNames']
      )

      util.registerBinding( dm )
      DomainHero object = util.dynamicBind(new Hero(name: 'Hulk', lastName: 'Banner', age: -1, otherNames: ['Bruce Banner'] ), DomainHero)

    then:
      object.name == 'Hulk'
      object.age == null
      object.lastName == 'Banner'

  }

  @Issue(value = '7')
  def 'should avoid read only properties '() {
    when:
      def origin = new Superpower( name: 'Levitate')
      def object = Util.bind(origin, DomainSuperpower)

    then:
      // Description is a readOnly property
      origin.properties.containsKey('description')
      object.name == 'Levitate'
  }

  @Issue(value = '6')
  def 'It must avoid empty list arguments'() {
    when:
      List goals = ['Capture Pikachu', 'Not to die because hunger']

      def origin = new Enemy(name: 'Rocket Team', goals:goals)
      def destination = Util.bind(origin, Enemy)

    then:
      destination.name == 'Rocket Team'
      destination.goals == origin.goals
  }
  
  @Issue(value = '6')
  def 'It must bind empty list correctly'() {
    when:
      List<Enemy> enemies = []
      def origin = new Hero(name: 'Linterna verde', enemies:enemies)
      Hero destination = Util.bind(origin, Hero)

    then:
      destination.name == 'Linterna verde'
      destination.enemies.isEmpty()
  }


}
