package gex.serling.binding.domain

import grails.persistence.Entity

/**
 * Created by Tsunllly on 1/26/15.
 */
@Entity
class Hero {

  String id

  String name
  Integer age
  Boolean isInmortal

  Superpower superpower

  List<Enemy> enemies


  Planet planet
  
  Status status

  static mapping = {
    id generator: 'uuid2'
  }

  static constraints = {
    name blank: false, nullable: false
  }
}


