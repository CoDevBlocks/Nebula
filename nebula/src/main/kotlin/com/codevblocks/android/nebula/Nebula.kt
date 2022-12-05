package com.codevblocks.android.nebula

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.*
import kotlin.properties.ReadWriteProperty
import kotlin.random.Random
import kotlin.reflect.KProperty

class Nebula @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val MIN_FPS = 1
        private const val MAX_FPS = 140

        private const val DEFAULT_MIN_RADIUS_DP = 200
        private const val DEFAULT_MAX_RADIUS_DP = 250
        private const val DEFAULT_ROUNDNESS = 1F

        private const val DEFAULT_LAYERS_COUNT = 3
        private const val DEFAULT_FILL_COLOR = 0x20808080
        private const val DEFAULT_VERTICES_COUNT = 10

        private const val DEFAULT_FILL_ALPHA = Float.NaN
        private const val DEFAULT_STROKE_ALPHA = Float.NaN
        private const val DEFAULT_STROKE_WIDTH_DP = 1

        private const val DEFAULT_FPS = 24
        private const val DEFAULT_FRAME_VERTEX_TRANSLATION_DP = 1
        private const val DEFAULT_FRAME_LAYER_ROTATION = 0.0174F
    }

    private var debug: Boolean by uiProperty(false)
    private var debugDrawBounds: Boolean by uiProperty(false)
    private var debugDrawMinRadius: Boolean by uiProperty(false)
    private var debugDrawMaxRadius: Boolean by uiProperty(false)
    private var debugDrawSlices: Boolean by uiProperty(false)
    private var debugDrawVertices: Boolean by uiProperty(false)
    private var debugDrawVertexPath: Boolean by uiProperty(false)
    private var debugDrawControlPoints: Boolean by uiProperty(false)

    private var initialized: Boolean = false

    var minRadius: Int by uiProperty(rebuildLayersOnChange = true)
    var maxRadius: Int by uiProperty(updateMeasurementsOnChange = true, rebuildLayersOnChange = true)
    var roundness: Float by uiProperty(DEFAULT_ROUNDNESS, rebuildLayersOnChange = true)

    var layersCount: Int by uiProperty(DEFAULT_LAYERS_COUNT, rebuildLayersOnChange = true)

    var verticesCount: IntArray by interceptedUIProperty(IntArray(1) { DEFAULT_VERTICES_COUNT }, rebuildLayersOnChange = true) { value -> if (value.isEmpty()) IntArray(1) { DEFAULT_VERTICES_COUNT } else value }
    var fillColors: IntArray by interceptedUIProperty(IntArray(1) { DEFAULT_FILL_COLOR }, refreshLayersOnChange = true) { value -> if (value.isEmpty()) IntArray(1) { DEFAULT_FILL_COLOR } else value }
    var strokeColors: IntArray by uiProperty(IntArray(0), refreshLayersOnChange = true)
    var strokeWidth: Int by uiProperty()

    var fillAlpha: Float by uiProperty(DEFAULT_FILL_ALPHA)
    var strokeAlpha: Float by uiProperty(DEFAULT_STROKE_ALPHA)

    var fps: Int by interceptedUIProperty(DEFAULT_FPS) { value -> min(MAX_FPS, max(MIN_FPS, value)) }
    var frameVertexTranslation: Int by interceptedUIProperty(DEFAULT_FRAME_VERTEX_TRANSLATION_DP) { value -> max(0, value) }
    var frameLayerRotation: Float by interceptedUIProperty(DEFAULT_FRAME_LAYER_ROTATION) { value -> max(0F, value % 360) }

    init {
        val resources = context.resources;
        val displayMetrics = resources.displayMetrics

        minRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_MIN_RADIUS_DP.toFloat(), displayMetrics).toInt()
        maxRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_MAX_RADIUS_DP.toFloat(), displayMetrics).toInt()

        strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_STROKE_WIDTH_DP.toFloat(), displayMetrics).toInt()

        frameVertexTranslation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_FRAME_VERTEX_TRANSLATION_DP.toFloat(), displayMetrics).toInt()

        context.obtainStyledAttributes(attrs, R.styleable.Nebula, 0, 0).apply {
            debug = getBoolean(R.styleable.Nebula_debug, debug)
            debugDrawBounds = getBoolean(R.styleable.Nebula_debug_drawBounds, debugDrawBounds)
            debugDrawMinRadius = getBoolean(R.styleable.Nebula_debug_drawMinRadius, debugDrawMinRadius)
            debugDrawMaxRadius = getBoolean(R.styleable.Nebula_debug_drawMaxRadius, debugDrawMaxRadius)
            debugDrawSlices = getBoolean(R.styleable.Nebula_debug_drawSlices, debugDrawSlices)
            debugDrawVertices = getBoolean(R.styleable.Nebula_debug_drawVertices, debugDrawVertices)
            debugDrawVertexPath = getBoolean(R.styleable.Nebula_debug_drawVertexPath, debugDrawVertexPath)
            debugDrawControlPoints = getBoolean(R.styleable.Nebula_debug_drawControlPoints, debugDrawControlPoints)

            minRadius = getDimensionPixelSize(R.styleable.Nebula_minRadius, minRadius)
            maxRadius = getDimensionPixelSize(R.styleable.Nebula_maxRadius, maxRadius)
            roundness = getFraction(R.styleable.Nebula_roundness, 1, 1, roundness)

            layersCount = getInteger(R.styleable.Nebula_layersCount, layersCount)

            verticesCount = peekValue(R.styleable.Nebula_verticesCount)?.let { typedValue ->
                when (typedValue.type) {
                    TypedValue.TYPE_INT_DEC -> {
                        IntArray(1) { getInteger(R.styleable.Nebula_verticesCount, DEFAULT_VERTICES_COUNT) }
                    }
                    TypedValue.TYPE_REFERENCE -> {
                        val resourceId = typedValue.resourceId
                        when (resources.getResourceTypeName(resourceId)) {
                            "array" -> resources.getIntArray(resourceId)
                            else -> throw IllegalArgumentException("Unsupported resource '${resources.getResourceName(resourceId)}'")
                        }
                    }
                    TypedValue.TYPE_STRING -> {
                        getString(R.styleable.Nebula_verticesCount)?.let { stringValue ->
                            stringValue.replace(Regex("\\s"), "")
                                .split(",")
                                .map { numberString ->
                                    Integer.parseUnsignedInt(numberString)
                                }
                                .toIntArray()
                        }

                    }
                    else -> null
                }
            } ?: verticesCount

            fillColors = peekValue(R.styleable.Nebula_fillColors)?.let { typedValue ->
                when {
                    typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT -> {
                        IntArray(1) { getColor(R.styleable.Nebula_fillColors, 0) }
                    }
                    typedValue.type == TypedValue.TYPE_REFERENCE -> {
                        val resourceId = typedValue.resourceId
                        when (resources.getResourceTypeName(resourceId)) {
                            "array" -> resources.getIntArray(resourceId)
                            else -> throw IllegalArgumentException("Unsupported resource '${resources.getResourceName(resourceId)}'")
                        }
                    }
                    typedValue.type == TypedValue.TYPE_STRING -> {
                        getString(R.styleable.Nebula_fillColors)?.let { stringValue ->
                            stringValue.replace(Regex("[#\\s]"), "")
                                .split(",")
                                .map { colorString ->
                                    Integer.parseUnsignedInt(colorString, 16).let { color ->
                                        if (((color shr 24) and 0xff) != 0) color else color.or(0xFF000000.toInt())
                                    }
                                }
                                .toIntArray()
                        }

                    }
                    else -> null
                }
            } ?: fillColors

            strokeColors = peekValue(R.styleable.Nebula_strokeColors)?.let { typedValue ->
                when {
                    typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT -> {
                        IntArray(1) { getColor(R.styleable.Nebula_strokeColors, 0) }
                    }
                    typedValue.type == TypedValue.TYPE_REFERENCE -> {
                        val resourceId = typedValue.resourceId
                        when (resources.getResourceTypeName(resourceId)) {
                            "array" -> resources.getIntArray(resourceId)
                            else -> throw IllegalArgumentException("Unsupported resource '${resources.getResourceName(resourceId)}'")
                        }
                    }
                    typedValue.type == TypedValue.TYPE_STRING -> {
                        getString(R.styleable.Nebula_strokeColors)?.let { stringValue ->
                            stringValue.replace(Regex("[#\\s]"), "")
                                .split(",")
                                .map { colorString ->
                                    Integer.parseUnsignedInt(colorString, 16).let { color ->
                                        if (((color shr 24) and 0xff) != 0) color else color.or(0xFF000000.toInt())
                                    }
                                }
                                .toIntArray()
                        }

                    }
                    else -> null
                }
            } ?: strokeColors

            strokeWidth = getDimensionPixelSize(R.styleable.Nebula_strokeWidth, strokeWidth)

            fillAlpha = getFraction(R.styleable.Nebula_fillAlpha, 1, 1, fillAlpha)
            strokeAlpha = getFraction(R.styleable.Nebula_strokeAlpha, 1, 1, strokeAlpha)

            fps = getInteger(R.styleable.Nebula_fps, fps)
            frameVertexTranslation = getDimensionPixelSize(R.styleable.Nebula_frameVertexTranslation, frameVertexTranslation)
            frameLayerRotation = getFloat(R.styleable.Nebula_frameLayerRotation, frameLayerRotation)
        }.recycle()

        initialized = true
    }

    private val layers = ArrayList<Layer>(10)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val drawingBounds = RectF()
    private val drawingBoundsCenter = PointF()

    init {
        updateMeasurements()
        rebuildLayers()

        post(object : Runnable {
            override fun run() {
                move()
                postDelayed(this, (1000F/fps).toLong())
            }

        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)

        var height = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        if (widthMode != MeasureSpec.EXACTLY) {
            val preferredWidth = drawingBounds.width().toInt() +
                    paddingLeft +
                    paddingRight

            width = if (widthMode == MeasureSpec.AT_MOST) min(width, preferredWidth) else preferredWidth
        }

        if (heightMode != MeasureSpec.EXACTLY) {
            val preferredHeight = drawingBounds.height().toInt() +
                    paddingTop +
                    paddingBottom

            height = if (heightMode == MeasureSpec.AT_MOST) min(height, preferredHeight) else preferredHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        with (canvas) {
            save()
            clipRect(
                paddingLeft.toFloat(),
                paddingTop.toFloat(),
                (width - paddingRight).toFloat(),
                (height - paddingBottom).toFloat()
            )

            for (layer in layers) {
                save()
                rotate(layer.rotation, drawingBoundsCenter.x, drawingBoundsCenter.y)

                if (((layer.fillColor shr 24) and 0xff) > 0 || !fillAlpha.isNaN()) {
                    paint.style = Paint.Style.FILL
                    paint.color = if (fillAlpha.isNaN()) layer.fillColor else
                        layer.fillColor.and(0x00FFFFFF).or((255 * fillAlpha).toInt().shl(24))

                    drawPath(layer.path, paint)
                }

                if ((((layer.strokeColor shr 24) and 0xff) > 0 || !strokeAlpha.isNaN()) && strokeWidth > 0F ) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = strokeWidth.toFloat()
                    paint.color = if (strokeAlpha.isNaN()) layer.strokeColor else
                        layer.strokeColor.and(0x00FFFFFF).or((255 * strokeAlpha).toInt().shl(24))

                    drawPath(layer.path, paint)
                }

                restore()
            }

            restore()
        }

        if (debug) {
            drawDebug(canvas)
        }
    }

    private fun drawDebug(canvas: Canvas) {
        val drawingBoundsWidth = drawingBounds.width()
        val drawingBoundsHeight = drawingBounds.height()

        if (debugDrawBounds) {
            paint.style = Paint.Style.STROKE
            paint.color = 0xFFE0E0E0.toInt()
            paint.strokeWidth = 2F

            canvas.drawRect(drawingBounds, paint)
        }

        paint.style = Paint.Style.STROKE
        paint.color = 0xFFFF0000.toInt()
        paint.strokeWidth = 2F

        if (debugDrawMinRadius) {
            canvas.drawCircle(drawingBoundsCenter.x, drawingBoundsCenter.y, minRadius.toFloat(), paint)
        }

        if (debugDrawMaxRadius) {
            canvas.drawCircle(drawingBoundsCenter.x, drawingBoundsCenter.y, maxRadius.toFloat(), paint)
        }

        var sliceAngle: Double
        var sweepAngle: Double

        for (layer in layers) {
            if (debugDrawSlices) {
                paint.style = Paint.Style.STROKE
                paint.color = 0xFFE0E0E0.toInt()
                paint.strokeWidth = 2F

                sliceAngle = 2 * PI / layer.polygon.vertexCount
                sweepAngle = 0.0

                with (drawingBoundsCenter) {
                    repeat (layer.polygon.vertexCount) {
                        canvas.drawLine(
                            x,
                            y,
                            x + (cos(sweepAngle) * drawingBoundsWidth).toFloat(),
                            y + (sin(sweepAngle) * drawingBoundsHeight).toFloat(),
                            paint
                        )

                        sweepAngle += sliceAngle
                    }
                }
            }

            with (canvas) {
                save()
                rotate(layer.rotation, drawingBoundsCenter.x, drawingBoundsCenter.y)

                if (debugDrawVertices) {
                    paint.style = Paint.Style.FILL
                    paint.color = layer.fillColor
                    for (vertex in layer.polygon) {
                        with (vertex) {
                            drawCircle(
                                point.x,
                                point.y,
                                16F,
                                paint
                            )
                        }
                    }
                }

                paint.style = Paint.Style.STROKE

                for (vertexAttributes in layer.verticesAttributes) {
                    with (vertexAttributes) {
                        if (debugDrawVertexPath) {
                            paint.color = 0xFF00FFFF.toInt()

                            with (drawingBoundsCenter) {
                                drawLine(
                                    x,
                                    y,
                                    x + (cos(centerAngle) * drawingBounds.width()).toFloat(),
                                    y + (sin(centerAngle) * drawingBounds.height()).toFloat(),
                                    paint
                                )
                            }
                        }

                        if (debugDrawControlPoints) {
                            paint.color = 0xFF00FF00.toInt()
                            drawCircle(controlPoint1.x, controlPoint1.y, 8F, paint)

                            paint.color = 0xFF0000FF.toInt()
                            drawCircle(controlPoint2.x, controlPoint2.y, 8F, paint)
                        }
                    }
                }

                restore()
            }
        }
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        updateMeasurements()
    }

    override fun setPaddingRelative(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPaddingRelative(left, top, right, bottom)
        updateMeasurements()
    }

    private fun updateMeasurements() {
        if (!initialized) {
            return
        }

        drawingBounds.set(0F, 0F, maxRadius * 2F, maxRadius * 2F)
        drawingBounds.offset(paddingLeft.toFloat(), paddingTop.toFloat())
        drawingBoundsCenter.set(drawingBounds.centerX(), drawingBounds.centerY())

        requestLayout()
    }

    private fun rebuildLayers() {
        if (!initialized) {
            return
        }

        layers.clear()

        repeat(layersCount) {
            layers.add(Layer(
                verticesCount[min(layers.size, verticesCount.lastIndex)],
                fillColors[min(layers.size, fillColors.lastIndex)],
                if (!strokeColors.isEmpty()) strokeColors[min(layers.size, strokeColors.lastIndex)] else
                    fillColors[min(layers.size, fillColors.lastIndex)]
            ))
        }

        invalidate()
    }

    private fun refreshLayers() {
        if (!initialized) {
            return
        }

        for ((index, layer) in layers.withIndex()) {
            with (layer) {
                fillColor = fillColors[min(index, fillColors.lastIndex)]
                strokeColor = if (!strokeColors.isEmpty()) strokeColors[min(index, strokeColors.lastIndex)] else
                    fillColors[min(index, fillColors.lastIndex)]
            }
        }

        invalidate()
    }

    private fun move() {
        for (layer in layers) {
            layer.move()
        }
        invalidate()
    }

    private class Vertex(var index: Int = 0) {

        val point: PointF = PointF(Float.NaN, Float.NaN)

        var previous: Vertex = this
        var next: Vertex = this

    }

    private class Polygon(val vertexCount: Int): Iterable<Vertex> {

        val startVertex: Vertex

        init {
            if (vertexCount < 3) {
                throw IllegalArgumentException("Invalid vertex count $vertexCount.")
            }

            startVertex = Vertex(0)

            repeat (vertexCount - 1) { iteration ->
                Vertex(iteration + 1).apply {
                    next = startVertex
                    previous = startVertex.previous
                    startVertex.previous.next = this
                    startVertex.previous = this
                }
            }
        }

        override fun iterator(): ListIterator<Vertex> {
            return VertexIterator()
        }

        private inner class VertexIterator: ListIterator<Vertex> {

            private var start = startVertex
            private var current = Vertex().apply {
                index = -1
                next = start
                previous = start.previous
            }

            override fun hasNext(): Boolean = current.index == -1 || current.next != start
            override fun nextIndex(): Int = current.next.index
            override fun next(): Vertex = current.next.also { current = current.next }

            override fun hasPrevious(): Boolean = current.index == -1 || current.previous != start
            override fun previousIndex(): Int = current.previous.index
            override fun previous(): Vertex = current.previous.also { current = current.previous }

        }

    }

    private data class VertexAttributes(
        var centerAngle: Double = Double.NaN,
        var distance: Float = Float.NaN,
        var direction: Byte = 0,
        val controlPoint1: PointF = PointF(Float.NaN, Float.NaN),
        val controlPoint2: PointF = PointF(Float.NaN, Float.NaN)
    )

    private inner class Layer(
        vertexCount: Int,
        var fillColor: Int,
        var strokeColor: Int = fillColor
    ) {

        var rotation = 0F
        private val rotationDirection: Byte

        val polygon = Polygon(vertexCount)
        val verticesAttributes = Array(vertexCount) { VertexAttributes() }
        val path = Path()

        private val Vertex.attributes: VertexAttributes get() = verticesAttributes[index]

        init {
            val random = Random.Default

            rotation = 360F * random.nextFloat()
            rotationDirection = if (random.nextBoolean()) 1 else -1

            val sliceAngle = 2 * PI / polygon.vertexCount
            var sweepAngle = 0.0

            for (vertex in polygon) {
                with (vertex) {
                    with (attributes) {
                        centerAngle = sweepAngle + sliceAngle * 0.5
                        distance = minRadius + (random.nextFloat() * (maxRadius - minRadius))
                        direction = if (random.nextBoolean()) 1 else -1

                        point.x = drawingBoundsCenter.x + (cos(centerAngle) * distance).toFloat()
                        point.y = drawingBoundsCenter.y + (sin(centerAngle) * distance).toFloat()
                    }
                }

                sweepAngle += sliceAngle
            }

            rebuildPath()
        }

        fun move() {
            rotation = (rotation + rotationDirection * frameLayerRotation) % 360F

            for (vertex in polygon) {
                with (vertex) {
                    with (attributes) {
                        distance = min(maxRadius.toFloat(), max(minRadius.toFloat(), distance + direction * frameVertexTranslation))

                        when (distance) {
                            minRadius.toFloat() -> direction = 1
                            maxRadius.toFloat() -> direction = -1
                        }

                        point.x = drawingBoundsCenter.x + (cos(centerAngle) * distance).toFloat()
                        point.y = drawingBoundsCenter.y + (sin(centerAngle) * distance).toFloat()
                    }
                }
            }

            rebuildPath()
        }

        fun rebuildPath() {
            var controlAngle: Float
            var controlAmplitude: Float
            var previousX: Float
            var previousY: Float
            var nextX: Float
            var nextY: Float

            for (vertex in polygon) {
                with (vertex) {
                    previousX = previous.point.x
                    previousY = previous.point.y
                    nextX = next.point.x
                    nextY = next.point.y

                    with (vertex.attributes) {
                        controlAmplitude = roundness * sqrt((previousX - nextX).pow(2) + (previousY - nextY).pow(2)) / 4F
                        controlAngle = atan2(previousY - nextY, previousX - nextX)

                        controlPoint1.x = point.x + cos(controlAngle) * controlAmplitude
                        controlPoint1.y = point.y + sin(controlAngle) * controlAmplitude

                        controlAngle += PI.toFloat()

                        controlPoint2.x = point.x + cos(controlAngle) * controlAmplitude
                        controlPoint2.y = point.y + sin(controlAngle) * controlAmplitude
                    }
                }
            }

            path.reset()

            var vertex = polygon.startVertex
            path.moveTo(vertex.point.x, vertex.point.y)

            do {
                with (vertex) {
                    with (vertex.attributes) {
                        path.cubicTo(
                            controlPoint2.x,
                            controlPoint2.y,
                            next.attributes.controlPoint1.x,
                            next.attributes.controlPoint1.y,
                            next.point.x,
                            next.point.y
                        )
                    }
                }

                vertex = vertex.next
            } while (vertex != polygon.startVertex)
        }

    }

    private open class UIPropertyDelegate<T>(
        private var value: Any? = null,
        private val updateMeasurementsOnChange: Boolean = false,
        private val rebuildLayersOnChange: Boolean = false,
        private val refreshLayersOnChange: Boolean = false
    ) : ReadWriteProperty<Nebula, T> {

        override fun getValue(thisRef: Nebula, property: KProperty<*>): T {
            @Suppress("UNCHECKED_CAST")
            return value as T
        }

        override fun setValue(thisRef: Nebula, property: KProperty<*>, value: T) {
            this.value = value

            if (updateMeasurementsOnChange) thisRef.updateMeasurements()
            if (rebuildLayersOnChange) thisRef.rebuildLayers() else
                if (refreshLayersOnChange) thisRef.refreshLayers()

            thisRef.invalidate()
        }

    }

    private fun <T> uiProperty(
        value: Any? = null,
        updateMeasurementsOnChange: Boolean = false,
        rebuildLayersOnChange: Boolean = false,
        refreshLayersOnChange: Boolean = false
    ): UIPropertyDelegate<T> {
        return UIPropertyDelegate(value, updateMeasurementsOnChange, rebuildLayersOnChange, refreshLayersOnChange)
    }

    private inline fun <T> interceptedUIProperty(
        value: T? = null,
        updateMeasurementsOnChange: Boolean = false,
        rebuildLayersOnChange: Boolean = false,
        refreshLayersOnChange: Boolean = false,
        crossinline intercept: (T) -> T
    ): UIPropertyDelegate<T> {
        return object : UIPropertyDelegate<T>(value, updateMeasurementsOnChange, rebuildLayersOnChange, refreshLayersOnChange) {
            override fun setValue(thisRef: Nebula, property: KProperty<*>, value: T) {
                super.setValue(thisRef, property, intercept(value))
            }
        }
    }

    private inline fun <T> reactiveUIProperty(
        value: T? = null,
        updateMeasurementsOnChange: Boolean = false,
        rebuildLayersOnChange: Boolean = false,
        refreshLayersOnChange: Boolean = false,
        crossinline reaction: () -> Unit
    ): UIPropertyDelegate<T> {
        return object : UIPropertyDelegate<T>(value, updateMeasurementsOnChange, rebuildLayersOnChange, refreshLayersOnChange) {
            override fun setValue(thisRef: Nebula, property: KProperty<*>, value: T) {
                super.setValue(thisRef, property, value)
                reaction()
            }
        }
    }

}