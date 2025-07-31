package com.astralplayer.nextplayer.professional

import android.graphics.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnotationManager @Inject constructor() {
    
    /**
     * Render annotations and measurements on a bitmap
     */
    fun renderAnnotations(
        originalBitmap: Bitmap,
        annotations: List<ProfessionalVideoToolsEngine.Annotation>,
        measurements: List<ProfessionalVideoToolsEngine.Measurement>
    ): Bitmap {
        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        
        // Render measurements first (behind annotations)
        measurements.forEach { measurement ->
            renderMeasurement(canvas, measurement)
        }
        
        // Render annotations on top
        annotations.forEach { annotation ->
            renderAnnotation(canvas, annotation)
        }
        
        return mutableBitmap
    }
    
    private fun renderMeasurement(
        canvas: Canvas,
        measurement: ProfessionalVideoToolsEngine.Measurement
    ) {
        when (measurement.type) {
            ProfessionalVideoToolsEngine.MeasurementType.DISTANCE -> {
                renderDistanceMeasurement(canvas, measurement)
            }
            ProfessionalVideoToolsEngine.MeasurementType.ANGLE -> {
                renderAngleMeasurement(canvas, measurement)
            }
            ProfessionalVideoToolsEngine.MeasurementType.AREA -> {
                renderAreaMeasurement(canvas, measurement)
            }
        }
    }
    
    private fun renderDistanceMeasurement(
        canvas: Canvas,
        measurement: ProfessionalVideoToolsEngine.Measurement
    ) {
        if (measurement.points.size < 2) return
        
        val paint = Paint().apply {
            color = Color.CYAN
            strokeWidth = 3f
            isAntiAlias = true
        }
        
        val textPaint = Paint().apply {
            color = Color.CYAN
            textSize = 24f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val point1 = measurement.points[0]
        val point2 = measurement.points[1]
        
        // Draw line
        canvas.drawLine(point1.x, point1.y, point2.x, point2.y, paint)
        
        // Draw end points
        canvas.drawCircle(point1.x, point1.y, 6f, paint)
        canvas.drawCircle(point2.x, point2.y, 6f, paint)
        
        // Draw measurement text
        val midX = (point1.x + point2.x) / 2f
        val midY = (point1.y + point2.y) / 2f
        val text = String.format("%.2f %s", measurement.realWorldValue, measurement.unit)
        
        // Background for text
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val backgroundPaint = Paint().apply {
            color = Color.BLACK
            alpha = 128
        }
        canvas.drawRect(
            midX - textBounds.width() / 2f - 4f,
            midY - textBounds.height() / 2f - 4f,
            midX + textBounds.width() / 2f + 4f,
            midY + textBounds.height() / 2f + 4f,
            backgroundPaint
        )
        
        canvas.drawText(text, midX - textBounds.width() / 2f, midY + textBounds.height() / 2f, textPaint)
    }
    
    private fun renderAngleMeasurement(
        canvas: Canvas,
        measurement: ProfessionalVideoToolsEngine.Measurement
    ) {
        if (measurement.points.size < 3) return
        
        val paint = Paint().apply {
            color = Color.YELLOW
            strokeWidth = 3f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
        
        val textPaint = Paint().apply {
            color = Color.YELLOW
            textSize = 24f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val point1 = measurement.points[0]
        val vertex = measurement.points[1]
        val point2 = measurement.points[2]
        
        // Draw lines from vertex to both points
        canvas.drawLine(vertex.x, vertex.y, point1.x, point1.y, paint)
        canvas.drawLine(vertex.x, vertex.y, point2.x, point2.y, paint)
        
        // Draw arc to show angle
        val radius = 50f
        val angle1 = kotlin.math.atan2((point1.y - vertex.y).toDouble(), (point1.x - vertex.x).toDouble())
        val angle2 = kotlin.math.atan2((point2.y - vertex.y).toDouble(), (point2.x - vertex.x).toDouble())
        
        val startAngle = Math.toDegrees(kotlin.math.min(angle1, angle2)).toFloat()
        val sweepAngle = Math.toDegrees(kotlin.math.abs(angle2 - angle1)).toFloat()
        
        val rect = RectF(
            vertex.x - radius,
            vertex.y - radius,
            vertex.x + radius,
            vertex.y + radius
        )
        
        canvas.drawArc(rect, startAngle, sweepAngle, false, paint)
        
        // Draw vertex point
        canvas.drawCircle(vertex.x, vertex.y, 6f, paint)
        
        // Draw angle text
        val text = String.format("%.1f°", measurement.realWorldValue)
        val textX = vertex.x + radius + 10f
        val textY = vertex.y
        
        canvas.drawText(text, textX, textY, textPaint)
    }
    
    private fun renderAreaMeasurement(
        canvas: Canvas,
        measurement: ProfessionalVideoToolsEngine.Measurement
    ) {
        if (measurement.points.size < 3) return
        
        val fillPaint = Paint().apply {
            color = Color.GREEN
            alpha = 64 // Semi-transparent
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        val strokePaint = Paint().apply {
            color = Color.GREEN
            strokeWidth = 3f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
        
        val textPaint = Paint().apply {
            color = Color.GREEN
            textSize = 24f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        
        // Create path for polygon
        val path = Path()
        val points = measurement.points
        
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        path.close()
        
        // Draw filled polygon
        canvas.drawPath(path, fillPaint)
        
        // Draw polygon outline
        canvas.drawPath(path, strokePaint)
        
        // Draw vertices
        points.forEach { point ->
            canvas.drawCircle(point.x, point.y, 6f, strokePaint)
        }
        
        // Calculate center for text
        val centerX = points.map { it.x }.average().toFloat()
        val centerY = points.map { it.y }.average().toFloat()
        
        // Draw area text
        val text = String.format("%.2f %s", measurement.realWorldValue, measurement.unit)
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        
        canvas.drawText(
            text,
            centerX - textBounds.width() / 2f,
            centerY + textBounds.height() / 2f,
            textPaint
        )
    }
    
    private fun renderAnnotation(
        canvas: Canvas,
        annotation: ProfessionalVideoToolsEngine.Annotation
    ) {
        when (annotation.type) {
            ProfessionalVideoToolsEngine.AnnotationType.TEXT -> {
                renderTextAnnotation(canvas, annotation)
            }
            ProfessionalVideoToolsEngine.AnnotationType.ARROW -> {
                renderArrowAnnotation(canvas, annotation)
            }
            ProfessionalVideoToolsEngine.AnnotationType.CIRCLE -> {
                renderCircleAnnotation(canvas, annotation)
            }
            ProfessionalVideoToolsEngine.AnnotationType.RECTANGLE -> {
                renderRectangleAnnotation(canvas, annotation)
            }
            ProfessionalVideoToolsEngine.AnnotationType.DRAWING -> {
                renderDrawingAnnotation(canvas, annotation)
            }
        }
    }
    
    private fun renderTextAnnotation(
        canvas: Canvas,
        annotation: ProfessionalVideoToolsEngine.Annotation
    ) {
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val backgroundPaint = Paint().apply {
            color = Color.BLACK
            alpha = 180
        }
        
        val textBounds = Rect()
        textPaint.getTextBounds(annotation.text, 0, annotation.text.length, textBounds)
        
        // Draw background
        canvas.drawRect(
            annotation.position.x - 8f,
            annotation.position.y - textBounds.height() - 8f,
            annotation.position.x + textBounds.width() + 8f,
            annotation.position.y + 8f,
            backgroundPaint
        )
        
        // Draw text
        canvas.drawText(annotation.text, annotation.position.x, annotation.position.y, textPaint)
    }
    
    private fun renderArrowAnnotation(
        canvas: Canvas,
        annotation: ProfessionalVideoToolsEngine.Annotation
    ) {
        val paint = Paint().apply {
            color = Color.RED
            strokeWidth = 4f
            isAntiAlias = true
            style = Paint.Style.FILL_AND_STROKE
        }
        
        val arrowLength = 60f
        val arrowWidth = 20f
        
        val endX = annotation.position.x + arrowLength
        val endY = annotation.position.y
        
        // Draw arrow shaft
        canvas.drawLine(annotation.position.x, annotation.position.y, endX, endY, paint)
        
        // Draw arrow head
        val path = Path()
        path.moveTo(endX, endY)
        path.lineTo(endX - arrowWidth, endY - arrowWidth / 2f)
        path.lineTo(endX - arrowWidth, endY + arrowWidth / 2f)
        path.close()
        
        canvas.drawPath(path, paint)
    }
    
    private fun renderCircleAnnotation(
        canvas: Canvas,
        annotation: ProfessionalVideoToolsEngine.Annotation
    ) {
        val paint = Paint().apply {
            color = Color.MAGENTA
            strokeWidth = 3f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
        
        canvas.drawCircle(annotation.position.x, annotation.position.y, 30f, paint)
    }
    
    private fun renderRectangleAnnotation(
        canvas: Canvas,
        annotation: ProfessionalVideoToolsEngine.Annotation
    ) {
        val paint = Paint().apply {
            color = Color.BLUE
            strokeWidth = 3f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
        
        canvas.drawRect(
            annotation.position.x - 30f,
            annotation.position.y - 20f,
            annotation.position.x + 30f,
            annotation.position.y + 20f,
            paint
        )
    }
    
    private fun renderDrawingAnnotation(
        canvas: Canvas,
        annotation: ProfessionalVideoToolsEngine.Annotation
    ) {
        annotation.drawingPath?.let { path ->
            annotation.drawingPaint?.let { paint ->
                canvas.drawPath(path, paint)
            }
        }
    }
}