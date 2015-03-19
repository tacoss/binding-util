package gex.serling.binding

/**
 * Created by Tsunllly on 2/19/15.
 */
class DynamicMapping {

  Class sourceClass
  Class destinationClass

  Map<String, Closure> customBindings
  List<String> exclusions

  DynamicMapping() {
    this.customBindings = [:]
    exclusions = []
  }
}
