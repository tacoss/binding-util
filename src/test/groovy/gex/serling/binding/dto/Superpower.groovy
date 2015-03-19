package gex.serling.binding.dto

/**
 * Created by Tsunllly on 1/27/15.
 */
class Superpower {
  String name

  // This is a readonly property
  String getDescription(){
    return {"hardcoded description for $name"}
  }

}
