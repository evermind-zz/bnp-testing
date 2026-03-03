package coil3

class ComponentRegistry private constructor() {

    class Builder {
        fun add(factory: Any): Builder = this
        fun build(): ComponentRegistry = ComponentRegistry()
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}
