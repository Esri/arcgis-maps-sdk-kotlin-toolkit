 # Composable Map[Deprecated]
 This module is deprecated. Use https://github.com/Esri/arcgis-maps-sdk-kotlin-toolkit/tree/main/toolkit/geoview-compose instead.

## DuplexFlow

Some properties on the `ComposableMap` that need to be observed and updated are exposed via a `DuplexFlow`.
This section provides its proper usage.

When it becomes necessary to both read and set a property such as `Map`'s rotation using `MapView.mapRotation` to read and `MapView.setViewpointRotation`
to set the value, it would result in a feedback loop if a single State is used to achieve this behavior.
The `DuplexFlow` is designed to solve this problem by encapsulating two distinct `Flow`'s who are individually responsible for Reading a property value and setting a property value. 

You should only emit/push to the READ flow, values that you intend to read. 
Similarly, only push values to the WRITE flow that are intended to be set on the MapView. 
There is no piping mechanism that feeds across the duplex. Hence the setter is usually exposed as a SharedFlow with a replay cache of 1.

### Usage

```kotlin
// create a private mutable backing property using the Factory MutableDuplexFlow()
private val _property: MutableDuplexFlow<Type?> = MutableDuplexFlow(null)
// cast it to an immutable DuplexFlow
public val property: DuplexFlow<Type> = _property

// to set value on the READ duplex flow
_property.setValue(value = newValue, duplex = Duplex.Read)
// to set value on the WRITE duplex flow
_property.setValue(value = newValue, duplex = Duplex.Write)

// to collect from the READ duplex flow
suspend fun Foo() {
    property.collect(Duplex.Read) { value ->
        ....
    }
}

// to collect from the WRITE duplex flow
suspend fun Bar() {
    property.collect(Duplex.Write) { value ->
        ....
    }
}
```




