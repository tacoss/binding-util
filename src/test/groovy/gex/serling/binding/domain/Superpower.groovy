package gex.serling.binding.domain

import grails.persistence.Entity

/**
 * Created by Tsunllly on 1/26/15.
 */
@Entity
class Superpower {

  String id

  String name

  static mapping = {
    id generator: 'uuid2'
  }

  static constraints = {
    name blank: false, nullable: false
  }

}
