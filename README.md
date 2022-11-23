If you like what I do and want to support me, you can

<a href="https://www.buymeacoffee.com/cosminradu" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 60px !important;width: 217px !important;" ></a>

# Nebula

<!-- ![Maven Central](https://img.shields.io/maven-central/v/com.codevblocks.android/nebula) -->
![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fcom%2Fcodevblocks%2Fandroid%2Fnebula%2Fmaven-metadata.xml)
![Kotlin Language](https://img.shields.io/badge/language-kotlin-blueviolet.svg)
![Kotlin Language](https://img.shields.io/badge/platform-android-green.svg)

Simple Android View resembling a [Nebula](https://en.wikipedia.org/wiki/Nebula). The effect is obtained by overlapping multiple rounded paths (layers). Each layer is based on a random polygon with vertices spread evenly along an imaginary circle with connecting cubic Bézier curves. There are two animations used to obtain the nebula effect: a rotation of the layer pivoting around the imaginary center, combined with a translation of each vertex along it's radius with a minimum and maximum defined distance from the imaginary center (see [Debug](https://github.com/CoDevBlocks/Nebula#debug) section below for a visual explanation).

![Nebula Sample 1](/media/nebula_01.gif)
![Nebula Sample 2](/media/nebula_02.gif)
![Nebula Sample 3](/media/nebula_03.gif)
![Nebula Sample 4](/media/nebula_04.gif)

## Sample in-app usage

![Screen Recording](/media/nebula_screenrec.gif)

## Installation

Simply add the dependency to your module `build.gradle` file

```groovy
dependencies {
    // ...
    implementation 'com.codevblocks.android:nebula:<version>'
    // ...
}
```
Current version available on [Maven](https://search.maven.org/artifact/com.codevblocks.android/nebula)

![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fcom%2Fcodevblocks%2Fandroid%2Fnebula%2Fmaven-metadata.xml)

## Usage

Include the `com.codevblocks.android.nebula.Nebula` view in your XML layout file. Here is a complete example:

```xml
<com.codevblocks.android.nebula.Nebula
    android:id="@+id/nebula"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"

    app:layersCount="5"

    app:minRadius="120dp"
    app:maxRadius="150dp"
    app:roundness="100%"

    app:verticesCount="12"
    app:fillColors="#3578C4,#FFEA80,#E6C000,#8EC0F9,#FAA1C2,#AC2E5C"
    app:fillAlpha="20%"
    app:strokeColors="@android:color/transparent"
    app:strokeAlpha="0%"
    app:strokeWidth="0dp"

    app:fps="30"
    app:frameVertexTranslation="1dp"
    app:frameLayerRotation="0"

    app:debug="false"
    app:debug_drawBounds="false"
    app:debug_drawMinRadius="false"
    app:debug_drawMaxRadius="false"
    app:debug_drawSlices="false"
    app:debug_drawVertices="false"
    app:debug_drawVertexPath="false"
    app:debug_drawControlPoints="false" />
```

## XML Attributes

#### `layersCount`
The number of rounded paths overlaid on top of each other
```
app:layersCount="5"
```
#### `minRadius`
The minimum distance a vertex is allowed to translate towards the imaginary Nebula center (see [Debug](https://github.com/CoDevBlocks/Nebula#debug))
```
app:minRadius="100dp"
```
#### `maxRadius`
The maximum distance a vertex is allowed to translate away from the imaginary Nebula center (see [Debug](https://github.com/CoDevBlocks/Nebula#debug))
```
app:maxRadius="150dp"
```
#### `roundness`
Property affecting the computation of the two Bézier control points used to draw the curve from one vertex to another. A value of 100% represents maximum roundness, while 0% means there is no roundness at all, meaning the layer is the polygon with straight lines in between vertices.
```
app:roundness="100%"
```

#### `verticesCount`
The number of vertices in the polygon defining each layer. Multiple comma separated values can be entered. If the number of values is less than the number of layers, the last entered value will be used for all subsequent layers. For example, if `verticesCount="8,10,12"` with 5 layers, layers 3, 4 and 5 will all have 12 vertices. Similarly, if only one value is defined, all layers will have that number of vertices. The minimum number of vertices in a polygon is 3

```
app:verticesCount="12"
```
```
app:verticesCount="12,10,8,10,12"
```

#### `fillColors`
Defines the layer fill colors. Multiple comma separated values can be entered (in `#rrggbb` and `#aarrggbb` format), as well as references to colors and color arrays. If the number of values is less than the number of layers, the last entered value will be used for all subsequent layers. For example, if `fillColors="#FF0000,#00FF00,#0000FF"` with 5 layers, layers 3, 4 and 5 will all have a `#0000FF` fill. Similarly, if only one value is defined, all layers will have that fill color.
```
app:fillColors="#3578C4"
```
```
app:fillColors="#3578C4,#FFEA80,#E6C000"
```
```
app:fillColors="@color/teal_200"
```
```
app:fillColors="@array/Nebula"
```

#### `fillAlpha`
When this attribute is specified, all layers will have the same alpha value applied to their fill, overriding the alpha of each color defined in `fillColors`
```
app:fillAlpha="20%"
```

#### `strokeColors`
Defines the layer stroke colors. Multiple comma separated values can be entered (in `#rrggbb` and `#aarrggbb` format), as well as references to colors and color arrays. If the number of values is less than the number of layers, the last entered value will be used for all subsequent layers. For example, if `strokeColors="#FF0000,#00FF00,#0000FF"` with 5 layers, layers 3, 4 and 5 will all have a `#0000FF` stroke. Similarly, if only one value is defined, all layers will have that stroke color.
```
app:strokeColors="#3578C4"
```
```
app:strokeColors="#3578C4,#FFEA80,#E6C000"
```
```
app:strokeColors="@color/teal_200"
```
```
app:strokeColors="@array/Nebula"
```

#### `strokeAlpha`
When this attribute is specified, all layers will have the same alpha value applied to their stroke, overriding the alpha of each color defined in `strokeColors`
```
app:strokeAlpha="20%"
```

#### `strokeWidth`
The width of the layer stroke
```
app:strokeWidth="1dp"
```

#### `fps`
The number of frames per second to be used when animating. A higher value translates to a preceived faster animation, while a lower value will be perceived as a slower animation. Upon each frame, the layers will be transformed according to `frameVertexTranslation` and `frameLayerRotation`.
```
app:fps="30"
```

#### `frameVertexTranslation`
The translation distance along the vertex path to be performed within each frame. A higher value translates to a preceived faster animation, while a lower value will be perceived as a slower animation.
```
app:frameVertexTranslation="1dp"
```
```
app:frameVertexTranslation="1px"
```

#### `frameLayerRotation`
The rotation (in degrees) around the Nebula center to be performed for each layer within each frame. A higher value translates to a preceived faster animation, while a lower value will be perceived as a slower animation.
```
app:frameLayerRotation="0.5"
```
```
app:frameLayerRotation="1"
```

## Debug

![Nebula Sample 1](/media/debug_01.gif)
![Nebula Sample 2](/media/debug_02.gif)

#### `debug`
Enables debugging mode. This must be set to `true` for any of the other debug flags to be taken into account.

#### `debug_drawBounds`
Renders the bounds of the drawable area

#### `debug_drawMinRadius`
Renders the circle defined by `minRadius`

#### `debug_drawMaxRadius`
Renders the circle defined by `maxRadius`

#### `debug_drawSlices`
Renders the equal circle slices used to evenly distribute the vertices of a layer

#### `debug_drawVertices`
Renders the layer polygon vertices

#### `debug_drawVertexPath`
Renders the path along which a vertex translates during the animation (in between `minRadius` and `maxRadius`)

#### `debug_drawControlPoints`
Renders the two cubic Bézier control points for each vertex