
0.1.15 / 2015-02-24
==================

  * Improves on readme file
  * Extending functionality for dynamic binding in order to support closures of type {val, obj -> val, obj}
  * Closure bindings support binding with property (default), whole object and even external parameters.


0.1.14 / 2015-02-23
==================

  * Changing from hsql to h2
  * IllegalArgumentException is thrown when source object is not an instance
  * Exclusions are now linked to source/destination classes
  * upgraded plugins and Groovy version
  * upgraded to Gradle 2.3
  * Dynamic binding was added. Tests included
  * Introducing dynamic bindings
  * Source refactor
  * The mechanism to support register of dynamic mappings was added. Static behaviour still works, but it will be deprecated

0.1.13 / 2015-01-27
==================

  * Removing spring boot plugin in the build file definition
  * Updating travis
  * Removing spaces
  * - Tests were added - Issue solved: Null value & False value support  (Binding boolean now works)
  * Tests now works, but i am not happy with grails version
  * Added Gitter badge

0.1.10 / 2014-12-22
==================

  * added support for bintray release
  * Merge pull request #12 from hgmiguel/master
  * fix missing property id
  * Upgrade version
  * fix for readOnly properties
  * Merge pull request #10 from hgmiguel/master
  * fix package name
  * Upgrade version
  * fix performance issues
  * Merge pull request #1 from gextech/master
  * Merge pull request #9 from gextech/feature/nullObjectsSupport
  * Merge pull request #8 from hgmiguel/master
  * Adding validation when source object is null
  * Upgrade version
  * Fix entity validation attributes
  * Upgrade version
  * Support unlimited camelcase close #5
  * Merge branch 'master' of https://github.com/hgmiguel/binding-util
  * avoid private repository
  * avoid private repository
  * Upgrade version
  * Fix domain to dto case & add enums support
  * Fix camel case dto validation
  * Add camel case on property class
  * Upgrade version
  * Add binding objects method
  * Apply idea plugin
  * first cut
  * initial project structure
  * Initial commit
