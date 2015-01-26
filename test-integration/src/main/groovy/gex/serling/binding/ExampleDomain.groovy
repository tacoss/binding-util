package gex.serling.binding

import grails.persistence.Entity

/**
 * Created by Tsunllly on 1/23/15.
 */
@Entity
class ExampleDomain {

  String id

  String name


  static mapping = {
    id generator: 'uuid2'
  }

  static constraints = {
    name blank: false, nullable: false
  }

}
