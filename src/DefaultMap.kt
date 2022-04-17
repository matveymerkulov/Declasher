class DefaultMap<Key, Value> {
  private val default: Value
  private val map: Map<Key, Value>

  constructor(default: Value, map: Map<Key, Value> = emptyMap()) {
    this.default = default
    this.map = map
  }

  operator fun get(key: Key): Value {
    return map[key] ?: default
  }
}