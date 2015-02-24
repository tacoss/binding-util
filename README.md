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

#### Pojo Classes

```groovy
package gex.serling.binding.dto

class Hero {
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

class Superpower {
  String name
}

class Enemy {
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
gex.serling.binding.domain.Hero hero = Util.bind(new Hero(name: 'The Doctor'), gex.serling.binding.domain.Hero)
assert hero.name == 'The Doctor'
```

Excluding properties from binding

```groovy
Hero dto = new Hero()
dto.name = "The doctor"
dto.age = 904

gex.serling.binding.domain.Hero domain = Util.bind(dto, gex.serling.binding.domain.Hero.class, ['age'])

assert domain.name == 'The Doctor'
assert domain.age == null
```

Inner properties binding using CamelCase. It works also for enums

```groovy
gex.serling.binding.domain.Hero domainHero = new gex.serling.binding.domain.Hero()
domainHero.name = "The doctor"
domainHero.superpower = new Superpower(name: 'Regeneration')
domainHero.status = Status.DELETED

Hero dtoHero = Util.bind(domainHero, Hero.class)

assert dtoHero.name == domainHero.name
assert dtoHero.superpowerName == domainHero.superpower.name
assert dtoHero.statusId == Status.DELETED.id
```

Binding lists :)

```groovy
gex.serling.binding.domain.Hero domainHero = new gex.serling.binding.domain.Hero()
domainHero.name = "The doctor"
domainHero.enemies = [new Enemy(name: 'Dalek'), new Enemy(name: 'Cyberman'), new Enemy(name: 'Weeping Angel')]

Hero dtoHero = Util.bind(domainHero, Hero.class)

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
  sourceClass: Hero.class,  
  destinationClass: gex.serling.binding.domain.Hero.class,
  customBindings: map 
)
util.registerBinding( cb )

def object = util.dynamicBind(new Hero(name: 'Goku', age: 21 ), gex.serling.binding.domain.Hero)

assert object.name == 'Goku'
assert object.age == 210
assert object.enemies.containsAll(hardcodedEnemies)
```

Default is over same property, bt binding can be defined over the whole object or external params (One, two or three params in closure)

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
  sourceClass: gex.serling.binding.domain.Hero.class,
  destinationClass: Hero.class,
  customBindings: mappings,
  exclusions: ["notPersistedField", "isInmortal"]
)

util.registerBinding( db )

gex.serling.binding.domain.Hero domainHero = new gex.serling.binding.domain.Hero()
domainHero.name = "The doctor"
domainHero.enemies = [new Enemy(name: 'Dalek'), new Enemy(name: 'Cyberman'), new Enemy(name: 'Weeping Angel')]
domainHero.age = 94
domainHero.isInmortal = true
domainHero.status = Status.ACTIVE
    
Hero dtoHero = util.dynamicBind(domainHero, Hero.class, extraParams)

dtoHero.name == "The doctor"
dtoHero.lastName == 'Smith'
dtoHero.enemies.containsAll(hardcodedEnemies)
dtoHero.age == 940
dtoHero.statusId == Status.ACTIVE.id
dtoHero.isInmortal == null
dtoHero.notPersistedField == null
dtoHero.separatedByCommaEnemies == "Dalek,Cyberman,Weeping Angel"
```


