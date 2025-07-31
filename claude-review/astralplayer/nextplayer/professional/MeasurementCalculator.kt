package com.astralplayer.nextplayer.professional

import android.graphics.PointF
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class MeasurementCalculator @Inject constructor() {
    
    /**
     * Calculate distance between two points in pixels
     */
    fun calculateDistance(point1: PointF, point2: PointF): Float {
        val dx = point2.x - point1.x
        val dy = point2.y - point1.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Calculate angle between three points (vertex is the middle point)
     */
    fun calculateAngle(point1: PointF, vertex: PointF, point2: PointF): Float {
        val vector1 = PointF(point1.x - vertex.x, point1.y - vertex.y)
        val vector2 = PointF(point2.x - vertex.x, point2.y - vertex.y)
        
        val dotProduct = vector1.x * vector2.x + vector1.y * vector2.y
        val magnitude1 = sqrt(vector1.x * vector1.x + vector1.y * vector1.y)
        val magnitude2 = sqrt(vector2.x * vector2.x + vector2.y * vector2.y)
        
        if (magnitude1 == 0f || magnitude2 == 0f) return 0f
        
        val cosAngle = dotProduct / (magnitude1 * magnitude2)
        val angleRadians = acos(cosAngle.coerceIn(-1f, 1f))
        
        return Math.toDegrees(angleRadians.toDouble()).toFloat()
    }
    
    /**
     * Calculate area of a polygon using the shoelace formula
     */
    fun calculatePolygonArea(points: List<PointF>): Float {
        if (points.size < 3) return 0f
        
        var area = 0f
        val n = points.size
        
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += points[i].x * points[j].y
            area -= points[j].x * points[i].y
        }
        
        return abs(area) / 2f
    }
    
    /**
     * Calculate perimeter of a polygon
     */
    fun calculatePolygonPerimeter(points: List<PointF>): Float {
        if (points.size < 2) return 0f
        
        var perimeter = 0f
        
        for (i in 0 until points.size) {
            val nextIndex = (i + 1) % points.size
            perimeter += calculateDistance(points[i], points[nextIndex])
        }
        
        return perimeter
    }
    
    /**
     * Calculate center point of a polygon
     */
    fun calculatePolygonCenter(points: List<PointF>): PointF {
        if (points.isEmpty()) return PointF(0f, 0f)
        
        var centerX = 0f
        var centerY = 0f
        
        for (point in points) {
            centerX += point.x
            centerY += point.y
        }
        
        return PointF(
            centerX / points.size,
            centerY / points.size
        )
    }
    
    /**
     * Calculate area of a circle
     */
    fun calculateCircleArea(center: PointF, radius: Float): Float {
        return PI.toFloat() * radius * radius
    }
    
    /**
     * Calculate circumference of a circle
     */
    fun calculateCircleCircumference(radius: Float): Float {
        return 2 * PI.toFloat() * radius
    }
    
    /**
     * Calculate area of an ellipse
     */
    fun calculateEllipseArea(majorAxis: Float, minorAxis: Float): Float {
        return PI.toFloat() * majorAxis * minorAxis
    }
    
    /**
     * Check if a point is inside a polygon
     */
    fun isPointInPolygon(point: PointF, polygon: List<PointF>): Boolean {
        if (polygon.size < 3) return false
        
        var inside = false
        var j = polygon.size - 1
        
        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]
            
            if ((pi.y > point.y) != (pj.y > point.y) &&
                point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x) {
                inside = !inside
            }
            j = i
        }
        
        return inside
    }
    
    /**
     * Calculate minimum bounding rectangle for a set of points
     */
    fun calculateBoundingRect(points: List<PointF>): BoundingRect {
        if (points.isEmpty()) return BoundingRect(0f, 0f, 0f, 0f)
        
        var minX = points[0].x
        var maxX = points[0].x
        var minY = points[0].y
        var maxY = points[0].y
        
        for (point in points) {
            minX = min(minX, point.x)
            maxX = max(maxX, point.x)
            minY = min(minY, point.y)
            maxY = max(maxY, point.y)
        }
        
        return BoundingRect(
            left = minX,
            top = minY,
            right = maxX,
            bottom = maxY
        )
    }
    
    /**
     * Convert pixel measurements to real-world units
     */
    fun convertToRealWorld(
        pixelValue: Float,
        pixelsPerUnit: Float,
        fromUnit: MeasurementUnit,
        toUnit: MeasurementUnit
    ): Float {
        // Convert to base unit (millimeters)
        val realWorldValue = pixelValue / pixelsPerUnit
        val baseValue = when (fromUnit) {
            MeasurementUnit.MILLIMETERS -> realWorldValue
            MeasurementUnit.CENTIMETERS -> realWorldValue * 10f
            MeasurementUnit.INCHES -> realWorldValue * 25.4f
            MeasurementUnit.PIXELS -> realWorldValue
        }
        
        // Convert from base unit to target unit
        return when (toUnit) {
            MeasurementUnit.MILLIMETERS -> baseValue
            MeasurementUnit.CENTIMETERS -> baseValue / 10f
            MeasurementUnit.INCHES -> baseValue / 25.4f
            MeasurementUnit.PIXELS -> baseValue * pixelsPerUnit
        }
    }
    
    /**
     * Calculate scale factor from known reference measurement
     */
    fun calculateScaleFactor(
        referencePixelLength: Float,
        referenceRealLength: Float,
        unit: MeasurementUnit
    ): Float {
        val unitMultiplier = when (unit) {
            MeasurementUnit.MILLIMETERS -> 1f
            MeasurementUnit.CENTIMETERS -> 10f
            MeasurementUnit.INCHES -> 25.4f
            MeasurementUnit.PIXELS -> 1f
        }
        
        return referenceRealLength * unitMultiplier / referencePixelLength
    }
    
    // Data classes
    data class BoundingRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
        val area: Float get() = width * height
        val center: PointF get() = PointF((left + right) / 2f, (top + bottom) / 2f)
    }
    
    enum class MeasurementUnit {
        PIXELS,
        MILLIMETERS,
        CENTIMETERS,
        INCHES
    }
}