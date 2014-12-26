package gex.serling.binding

import spock.lang.Specification

/**
 * Created by domix on 12/26/14.
 */
class UtilSpec extends Specification {
  def 'should bind a new Instance taking a instanciated object'() {
    when:
      def util = new Util()
      def object = util.bind(new Demo(name: 'name'), Demo)
    then:
      object.name == 'name'
  }
}

class Demo {
  String name
}