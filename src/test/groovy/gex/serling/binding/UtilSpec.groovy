package gex.serling.binding

import spock.lang.Specification

/**
 * Created by domix on 12/26/14.
 */
class UtilSpec extends Specification {
  def 'foo'() {
    when:
      def util = new Util()
    then:
      util
  }
}
