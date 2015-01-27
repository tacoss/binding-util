package gex.serling.binding.dto

import gex.serling.binding.domain.Superpower

/**
 * Created by Tsunllly on 1/23/15.
 */
class Hero {
  String name
  Integer age
  Boolean isInmortal
  String notPersistedField
  List otherNames

  String SuperpowerName


  List<Enemy> enemies


}