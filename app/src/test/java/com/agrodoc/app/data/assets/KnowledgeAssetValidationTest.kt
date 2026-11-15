package com.agrodoc.app.data.assets

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KnowledgeAssetValidationTest {

    private fun findAssetFile(path: String): File {
        val candidates = listOf(
            File(path),
            File("app/$path"),
            File("src/main/assets/$path"),
            File("app/src/main/assets/$path")
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalArgumentException("Cannot find asset file in candidates: $path")
    }

    @Test
    fun cropsJsonContainsRequiredCropsWithMultilingualFields() {
        val file = findAssetFile("src/main/assets/knowledge/crops.json")
        assertTrue("crops.json must exist in assets", file.exists())

        val element = Json.parseToJsonElement(file.readText())
        val array = element.jsonArray
        assertTrue("Should contain at least 25 Karnataka crops (found ${array.size})", array.size >= 25)

        for (item in array) {
            val crop = item.jsonObject
            assertTrue("Crop must have non-empty id", crop["id"]?.jsonPrimitive?.content.orEmpty().isNotEmpty())
            assertTrue("Crop must have English name", crop["nameEn"]?.jsonPrimitive?.content.orEmpty().isNotEmpty())
            assertTrue("Crop must have Kannada name", crop["nameKn"]?.jsonPrimitive?.content.orEmpty().isNotEmpty())
            assertTrue("Crop must have Hindi name", crop["nameHi"]?.jsonPrimitive?.content.orEmpty().isNotEmpty())
            assertTrue("Crop must have Marathi name", crop["nameMr"]?.jsonPrimitive?.content.orEmpty().isNotEmpty())
        }
    }

    @Test
    fun diseaseInfoJsonContainsComprehensiveDiseases() {
        val file = findAssetFile("src/main/assets/knowledge/disease_info.json")
        assertTrue("disease_info.json must exist in assets", file.exists())

        val element = Json.parseToJsonElement(file.readText())
        val array = element.jsonArray
        assertTrue("Should contain at least 30 diseases (found ${array.size})", array.size >= 30)

        for (item in array) {
            val disease = item.jsonObject
            assertTrue("Disease must have id", disease["id"]?.jsonPrimitive?.content.orEmpty().isNotEmpty())
            assertTrue("Disease must have cropKey", disease["cropKey"]?.jsonPrimitive?.content.orEmpty().isNotEmpty())
            assertTrue("Disease must have English name", disease["nameEn"]?.jsonPrimitive?.content.orEmpty().isNotEmpty())
            assertTrue("Disease must have Kannada name", disease["nameKn"]?.jsonPrimitive?.content.orEmpty().isNotEmpty())
        }
    }

    @Test
    fun districtsJsonContainsValidCentroids() {
        val file = findAssetFile("src/main/assets/districts.json")
        assertTrue("districts.json must exist in assets", file.exists())

        val element = Json.parseToJsonElement(file.readText())
        val array = element.jsonArray
        assertTrue("Should contain at least 50 districts across India", array.size >= 50)

        for (item in array) {
            val d = item.jsonObject
            val lat = d["lat"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: Double.NaN
            val lon = d["lon"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: Double.NaN
            assertTrue("Lat must be valid coordinate ($lat)", !lat.isNaN() && lat in 6.0..38.0)
            assertTrue("Lon must be valid coordinate ($lon)", !lon.isNaN() && lon in 68.0..98.0)
        }
    }
}
