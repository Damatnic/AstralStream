package com.astralplayer.nextplayer.professional

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Typeface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalysisExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val exportDirectory = File(context.getExternalFilesDir(null), "analysis_exports")
    
    init {
        exportDirectory.mkdirs()
    }
    
    suspend fun exportToPDF(
        analysisData: ProfessionalVideoToolsEngine.AnalysisData,
        includeFrames: Boolean = false
    ): ProfessionalVideoToolsEngine.ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "analysis_$timestamp.pdf"
                val filePath = File(exportDirectory, fileName).absolutePath
                
                // Create first page with summary
                createSummaryPage(pdfDocument, analysisData)
                
                // Create measurements page
                if (analysisData.measurements.isNotEmpty()) {
                    createMeasurementsPage(pdfDocument, analysisData.measurements)
                }
                
                // Create annotations page
                if (analysisData.annotations.isNotEmpty()) {
                    createAnnotationsPage(pdfDocument, analysisData.annotations)
                }
                
                // Write PDF to file
                val fileOutputStream = FileOutputStream(filePath)
                pdfDocument.writeTo(fileOutputStream)
                pdfDocument.close()
                fileOutputStream.close()
                
                ProfessionalVideoToolsEngine.ExportResult(
                    success = true,
                    filePath = filePath
                )
                
            } catch (e: Exception) {
                ProfessionalVideoToolsEngine.ExportResult(
                    success = false,
                    filePath = null,
                    error = e.message
                )
            }
        }
    }
    
    suspend fun exportToCSV(
        analysisData: ProfessionalVideoToolsEngine.AnalysisData
    ): ProfessionalVideoToolsEngine.ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "analysis_$timestamp.csv"
                val filePath = File(exportDirectory, fileName).absolutePath
                
                val writer = FileWriter(filePath)
                
                // Write header
                writer.append("Type,ID,Value,Unit,X1,Y1,X2,Y2,X3,Y3,Frame Position,Timestamp\n")
                
                // Write measurements
                analysisData.measurements.forEach { measurement ->
                    writer.append("Measurement,")
                    writer.append("${measurement.id},")
                    writer.append("${measurement.realWorldValue},")
                    writer.append("${measurement.unit},")
                    
                    // Write up to 3 points
                    for (i in 0 until 3) {
                        if (i < measurement.points.size) {
                            writer.append("${measurement.points[i].x},${measurement.points[i].y},")
                        } else {
                            writer.append(",,")
                        }
                    }
                    
                    writer.append("${measurement.framePosition},")
                    writer.append("${measurement.timestamp}\n")
                }
                
                // Write annotations
                analysisData.annotations.forEach { annotation ->
                    writer.append("Annotation,")
                    writer.append("${annotation.id},")
                    writer.append("\"${annotation.text}\",")
                    writer.append(",") // No unit for annotations
                    writer.append("${annotation.position.x},${annotation.position.y},")
                    writer.append(",,,,") // Empty X2,Y2,X3,Y3
                    writer.append("${annotation.framePosition},")
                    writer.append("${annotation.timestamp}\n")
                }
                
                writer.close()
                
                ProfessionalVideoToolsEngine.ExportResult(
                    success = true,
                    filePath = filePath
                )
                
            } catch (e: Exception) {
                ProfessionalVideoToolsEngine.ExportResult(
                    success = false,
                    filePath = null,
                    error = e.message
                )
            }
        }
    }
    
    suspend fun exportToJSON(
        analysisData: ProfessionalVideoToolsEngine.AnalysisData
    ): ProfessionalVideoToolsEngine.ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "analysis_$timestamp.json"
                val filePath = File(exportDirectory, fileName).absolutePath
                
                val jsonObject = JSONObject()
                
                // Add metadata
                jsonObject.put("exportTimestamp", analysisData.exportTimestamp)
                jsonObject.put("playbackSpeed", analysisData.playbackSpeed)
                jsonObject.put("mediaUri", analysisData.mediaItem.localConfiguration?.uri.toString())
                
                // Add measurements
                val measurementsArray = JSONArray()
                analysisData.measurements.forEach { measurement ->
                    val measurementObj = JSONObject()
                    measurementObj.put("id", measurement.id)
                    measurementObj.put("type", measurement.type.name)
                    measurementObj.put("pixelValue", measurement.pixelValue)
                    measurementObj.put("realWorldValue", measurement.realWorldValue)
                    measurementObj.put("unit", measurement.unit)
                    measurementObj.put("framePosition", measurement.framePosition)
                    measurementObj.put("timestamp", measurement.timestamp)
                    
                    val pointsArray = JSONArray()
                    measurement.points.forEach { point ->
                        val pointObj = JSONObject()
                        pointObj.put("x", point.x)
                        pointObj.put("y", point.y)
                        pointsArray.put(pointObj)
                    }
                    measurementObj.put("points", pointsArray)
                    
                    measurementsArray.put(measurementObj)
                }
                jsonObject.put("measurements", measurementsArray)
                
                // Add annotations
                val annotationsArray = JSONArray()
                analysisData.annotations.forEach { annotation ->
                    val annotationObj = JSONObject()
                    annotationObj.put("id", annotation.id)
                    annotationObj.put("type", annotation.type.name)
                    annotationObj.put("text", annotation.text)
                    annotationObj.put("framePosition", annotation.framePosition)
                    annotationObj.put("timestamp", annotation.timestamp)
                    
                    val positionObj = JSONObject()
                    positionObj.put("x", annotation.position.x)
                    positionObj.put("y", annotation.position.y)
                    annotationObj.put("position", positionObj)
                    
                    annotationsArray.put(annotationObj)
                }
                jsonObject.put("annotations", annotationsArray)
                
                // Write JSON to file
                val writer = FileWriter(filePath)
                writer.write(jsonObject.toString(2)) // Pretty print with 2-space indent
                writer.close()
                
                ProfessionalVideoToolsEngine.ExportResult(
                    success = true,
                    filePath = filePath
                )
                
            } catch (e: Exception) {
                ProfessionalVideoToolsEngine.ExportResult(
                    success = false,
                    filePath = null,
                    error = e.message
                )
            }
        }
    }
    
    private fun createSummaryPage(
        pdfDocument: PdfDocument,
        analysisData: ProfessionalVideoToolsEngine.AnalysisData
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.DEFAULT
        }
        
        var yPosition = 50f
        
        // Title
        canvas.drawText("AstralStream Video Analysis Report", 50f, yPosition, titlePaint)
        yPosition += 40f
        
        // Metadata
        canvas.drawText("Export Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(analysisData.exportTimestamp))}", 50f, yPosition, textPaint)
        yPosition += 25f
        
        canvas.drawText("Media: ${analysisData.mediaItem.localConfiguration?.uri?.lastPathSegment ?: "Unknown"}", 50f, yPosition, textPaint)
        yPosition += 25f
        
        canvas.drawText("Playback Speed: ${analysisData.playbackSpeed}x", 50f, yPosition, textPaint)
        yPosition += 40f
        
        // Summary statistics
        canvas.drawText("Summary:", 50f, yPosition, titlePaint)
        yPosition += 30f
        
        canvas.drawText("Total Measurements: ${analysisData.measurements.size}", 70f, yPosition, textPaint)
        yPosition += 20f
        
        val distanceCount = analysisData.measurements.count { it.type == ProfessionalVideoToolsEngine.MeasurementType.DISTANCE }
        canvas.drawText("- Distance Measurements: $distanceCount", 90f, yPosition, textPaint)
        yPosition += 20f
        
        val angleCount = analysisData.measurements.count { it.type == ProfessionalVideoToolsEngine.MeasurementType.ANGLE }
        canvas.drawText("- Angle Measurements: $angleCount", 90f, yPosition, textPaint)
        yPosition += 20f
        
        val areaCount = analysisData.measurements.count { it.type == ProfessionalVideoToolsEngine.MeasurementType.AREA }
        canvas.drawText("- Area Measurements: $areaCount", 90f, yPosition, textPaint)
        yPosition += 25f
        
        canvas.drawText("Total Annotations: ${analysisData.annotations.size}", 70f, yPosition, textPaint)
        
        pdfDocument.finishPage(page)
    }
    
    private fun createMeasurementsPage(
        pdfDocument: PdfDocument,
        measurements: List<ProfessionalVideoToolsEngine.Measurement>
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.DEFAULT
        }
        
        var yPosition = 50f
        
        canvas.drawText("Measurements Detail", 50f, yPosition, titlePaint)
        yPosition += 40f
        
        measurements.forEach { measurement ->
            canvas.drawText("${measurement.type.name}: ${String.format("%.2f", measurement.realWorldValue)} ${measurement.unit}", 50f, yPosition, textPaint)
            yPosition += 20f
            
            canvas.drawText("Frame: ${measurement.framePosition}ms", 70f, yPosition, textPaint)
            yPosition += 15f
            
            canvas.drawText("Points: ${measurement.points.size}", 70f, yPosition, textPaint)
            yPosition += 25f
            
            if (yPosition > 800f) break // Prevent overflow
        }
        
        pdfDocument.finishPage(page)
    }
    
    private fun createAnnotationsPage(
        pdfDocument: PdfDocument,
        annotations: List<ProfessionalVideoToolsEngine.Annotation>
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 3).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.DEFAULT
        }
        
        var yPosition = 50f
        
        canvas.drawText("Annotations Detail", 50f, yPosition, titlePaint)
        yPosition += 40f
        
        annotations.forEach { annotation ->
            canvas.drawText("${annotation.type.name}: ${annotation.text}", 50f, yPosition, textPaint)
            yPosition += 20f
            
            canvas.drawText("Frame: ${annotation.framePosition}ms", 70f, yPosition, textPaint)
            yPosition += 15f
            
            canvas.drawText("Position: (${String.format("%.1f", annotation.position.x)}, ${String.format("%.1f", annotation.position.y)})", 70f, yPosition, textPaint)
            yPosition += 25f
            
            if (yPosition > 800f) break // Prevent overflow
        }
        
        pdfDocument.finishPage(page)
    }
}