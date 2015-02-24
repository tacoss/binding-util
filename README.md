binding-util
============

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/gextech/binding-util?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This library binds Java Objects, it is aware of hibernate (GORM) entities

[![Build Status](https://travis-ci.org/gextech/binding-util.svg?branch=master)](https://travis-ci.org/gextech/binding-util)
[![Coverage Status](https://img.shields.io/coveralls/gextech/binding-util.svg)](https://coveralls.io/r/gextech/binding-util?branch=master)
[ ![Download](https://api.bintray.com/packages/gextech/oss/binding-util/images/download.svg) ](https://bintray.com/gextech/oss/binding-util/_latestVersion)


Include Dependency
--------------------

Gradle `gex.serling:binding-util:0.1.15`


Usage
------

Given:

#### Domain Classes

```groovy
package gex.serling.binding.domain

import grails.persistence.Entity

@Entity
class Hero {
  String id
  String name
  String lastName
  Integer age
  Boolean isInmortal
  Superpower superpower
  List<Enemy> enemies
  Planet planet
  Status status
}

@Entity
class Superpower {
  String id
  String name
}

@Entity
class Enemy {
  String id
  String name
}

public enum Status {
  ACTIVE(1, "Activo"),
  SUSPENDED(2, "Suspendido"),
  DELETED(3, "Eliminado")

  final int id
  final String description
  public Status(int id, String description) {
    this.id = id
    this.description = description
  }
  int value() { return id }
  static Status byId(int id) {
    values().find { it.id == id }
  }
  String getDescription() {
    description
  }
}

```

#### Dto Classes

```groovy
package gex.serling.binding.dto

class HeroDto {
  String name
  String lastName
  Integer age
  Boolean isInmortal
  String notPersistedField
  List otherNames
  String SuperpowerName
  Integer statusId
  List<Enemy> enemies
  String separatedByCommaEnemies
}

class EnemyDto {
  String name
}

```


### Simple static usage

Same type objects binding

```groovy
Hero hero = Util.bind(new Hero(name: 'The Doctor'), Hero)
assert hero.name == 'The Doctor'
```

Different type objects binding

```groovy
Hero hero = Util.bind(new HeroDto(name: 'The Doctor'), Hero)
assert hero.name == 'The Doctor'
```

Excluding properties from binding

```groovy
HeroDto dto = new HeroDto()
dto.name = "The doctor"
dto.age = 904

Hero domain = Util.bind(dto, Hero.class, ['age'])

assert domain.name == 'The Doctor'
assert domain.age == null
```

Inner properties binding using CamelCase. It works also for enums

```groovy
Hero domainHero = Hero()
domainHero.name = "The doctor"
domainHero.superpower = new Superpower(name: 'Regeneration')
domainHero.status = Status.DELETED

HeroDto dtoHero = Util.bind(domainHero, HeroDto.class)

assert dtoHero.name == domainHero.name
assert dtoHero.superpowerName == domainHero.superpower.name
assert dtoHero.statusId == Status.DELETED.id
```

Binding lists :)

```groovy
Hero domainHero = new Hero()
domainHero.name = "The doctor"
domainHero.enemies = [new Enemy(name: 'Dalek'), new Enemy(name: 'Cyberman'), new Enemy(name: 'Weeping Angel')]

HeroDto dtoHero = Util.bind(domainHero, HeroDto.class)

assert dtoHero.name == domainHero.name
assert dtoHero.enemies*.name.containsAll(domainHero.enemies*.name)
```

### Dynamic binding usage

Simple usage. Define once. Use everywhere

```groovy
def util = new Util()

def hardcodedEnemies = [new Enemy(name: 'OtroDale'), new Enemy(name: 'OtroCyberman'), new Enemy(name: 'Otro Weeping Ange')]

Map map = [
  "age" : { x -> x * 10 },
  "enemies" : { x -> hardcodedEnemies }
]

def cb = new DynamicMapping(
  sourceClass: HeroDto.class,
  destinationClass: Hero.class,
  customBindings: map
)
util.registerBinding( cb )

Hero object = util.dynamicBind(new HeroDto(name: 'Goku', age: 21 ), Hero)

assert object.name == 'Goku'
assert object.age == 210
assert object.enemies.containsAll(hardcodedEnemies)
```

Default is over same property, but binding can be defined over the whole object or external params (One, two or three params in closure)

```groovy
def util = new Util()

// Register bindings
def hardcodedEnemies = [new Enemy(name: 'Silence'), new Enemy(name: 'Dark')]

Map mappings = [
  "age" : { x, y -> x * 10 },
  "enemies" : { x -> hardcodedEnemies },
  "separatedByCommaEnemies" : {val, obj -> obj.enemies*.name.join(",")},
  "lastName": {val, obj, extra ->  extra[obj.name] }
 ]

def db = new DynamicMapping(
  sourceClass: Hero.class,
  destinationClass: HeroDto.class,
  customBindings: mappings,
  exclusions: ["notPersistedField", "isInmortal"]
)

util.registerBinding( db )

/* A first binding*/

Hero domainHero = new Hero()
domainHero.name = "The doctor"
domainHero.enemies = [new Enemy(name: 'Dalek'), new Enemy(name: 'Cyberman'), new Enemy(name: 'Weeping Angel')]
domainHero.age = 94
domainHero.isInmortal = true
domainHero.status = Status.ACTIVE

HeroDto dtoHero = util.dynamicBind(domainHero, HeroDto.class, extraParams)

dtoHero.name == "The doctor"
dtoHero.lastName == 'Smith'
dtoHero.enemies.containsAll(hardcodedEnemies)
dtoHero.age == 940
dtoHero.statusId == Status.ACTIVE.id
dtoHero.isInmortal == null
dtoHero.notPersistedField == null
dtoHero.separatedByCommaEnemies == "Dalek,Cyberman,Weeping Angel"

 /* A second binding */

domainHero = new Hero()
domainHero.name = "Pikachu"
dtoHero.lastName == 'Mon'
domainHero.enemies = [new Enemy(name: 'Jessy'), new Enemy(name: 'James')]
domainHero.age = 5
domainHero.isInmortal = false
domainHero.status = Status.SUSPENDED

dtoHero = util.dynamicBind(domainHero, HeroDto.class, extraParams)

dtoHero.name == "Pikachu"
dtoHero.enemies.containsAll(hardcodedEnemies)
dtoHero.age == 50
dtoHero.statusId == Status.SUSPENDED.id
dtoHero.isInmortal == null
dtoHero.notPersistedField == null
dtoHero.separatedByCommaEnemies == "Jessy,James"

```

So, it is posible to define and configure a bean with all necessary mappings and simply inject it. For example, in Spring Boot:

```groovy
@EnableAutoConfiguration
@Configuration
@ComponentScan
class TheConfigFile {

  @Bean
  Util getBindingUtil() {
    def util = new Util()

    Map mappings = [
      "age" : { x, y -> x * 10 },
      "separatedByCommaEnemies" : {val, obj -> obj.enemies*.name.join(",")},
      "lastName": {val, obj, extra ->  extra[obj.name] }
    ]
    def db = new DynamicMapping(
      sourceClass: Hero.class,
      destinationClass: HeroDto.class,
      customBindings: mappings,
      exclusions: ["notPersistedField", "isInmortal"]
    )
    util.registerBinding( db )

    util
  }

}
```

and then

```groovy
class WhateverClass{
  @Autowired
  Util bindingUtil

  ...
}
```






