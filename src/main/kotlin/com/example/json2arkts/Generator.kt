package com.example.json2arkts

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive

class Generator {
    private val generatedTypes = LinkedHashMap<String, String>() // Use LinkedHashMap to keep order
    private var useClass = false
    private var useObservedV2 = false

    fun generate(json: String, rootName: String = "Root", useClass: Boolean = false, useObservedV2: Boolean = false): String {
        this.useClass = useClass
        this.useObservedV2 = useObservedV2
        generatedTypes.clear()
        try {
            val element = JsonParser.parseString(json)
            if (element.isJsonObject) {
                parseObject(element.asJsonObject, rootName)
            } else if (element.isJsonArray) {
                val itemType = parseArray(element.asJsonArray, rootName)
                return "// Root is an Array of $itemType\n\n" + generatedTypes.values.joinToString("\n\n")
            }
        } catch (e: Exception) {
            return "// Error parsing JSON: ${e.message}"
        }

        return generatedTypes.values.joinToString("\n\n")
    }

    private fun parseObject(jsonObject: JsonObject, className: String): String {
        val sb = StringBuilder()
        val typeKeyword = if (useClass) "class" else "interface"
        
        if (useClass && useObservedV2) {
            sb.append("@ObservedV2\n")
        }
        sb.append("export $typeKeyword $className {\n")
        
        jsonObject.entrySet().forEach { entry ->
            val key = entry.key
            val value = entry.value
            val fieldName = key 
            
            val suggestedTypeName = toPascalCase(key)
            val typeName = getTypeName(value, suggestedTypeName)
            
            if (useClass) {
                val defaultValue = getDefaultValue(typeName)
                val tracePrefix = if (useObservedV2) "@Trace " else ""
                sb.append("  $tracePrefix$fieldName: $typeName = $defaultValue;\n")
            } else {
                sb.append("  $fieldName: $typeName;\n")
            }
        }
        sb.append("}")
        
        val code = sb.toString()
        generatedTypes[className] = code
        return className
    }
    
    private fun parseArray(jsonArray: JsonArray, keyName: String): String {
        if (jsonArray.size() == 0) return "any[]"
        val first = jsonArray.get(0)
        val singularName = if (keyName.endsWith("s")) keyName.dropLast(1) else keyName + "Item"
        val type = getTypeName(first, singularName)
        return "$type[]"
    }

    private fun getTypeName(element: com.google.gson.JsonElement, keyName: String): String {
        if (element.isJsonPrimitive) {
            val p = element.asJsonPrimitive
            if (p.isBoolean) return "boolean"
            if (p.isNumber) return "number"
            if (p.isString) return "string"
        } else if (element.isJsonObject) {
            return parseObject(element.asJsonObject, keyName)
        } else if (element.isJsonArray) {
             return parseArray(element.asJsonArray, keyName)
        }
        return "any"
    }

    private fun getDefaultValue(typeName: String): String {
        return when (typeName) {
            "string" -> "\"\""
            "number" -> "0"
            "boolean" -> "false"
            "any" -> "null"
            else -> {
                if (typeName.endsWith("[]")) "[]"
                else "new $typeName()"
            }
        }
    }

    private fun toPascalCase(text: String): String {
        // Handle common separators: underscore, hyphen, space
        // Also remove any non-alphanumeric characters to be safe for class names
        val safeText = text.replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
        return safeText.split("_", "-", " ").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}
