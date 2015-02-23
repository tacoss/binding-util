package gex.serling.binding.domain

/**
 * Created by Tsunllly on 2/20/15.
 */
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
