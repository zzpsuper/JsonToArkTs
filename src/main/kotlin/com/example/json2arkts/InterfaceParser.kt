package com.example.json2arkts

data class ParsedInterface(
    val name: String,
    val fields: List<Field>,
    val isClass: Boolean = false
)

data class Field(
    val name: String,
    val type: String,
    val isOptional: Boolean
)

class InterfaceParser {

    fun parse(text: String): ParsedInterface? {
        // 1. Try to find interface definition
        var nameRegex = Regex("""interface\s+(\w+)""")
        var nameMatch = nameRegex.find(text)
        var isClass = false

        // 2. If not interface, try to find class definition
        if (nameMatch == null) {
            nameRegex = Regex("""class\s+(\w+)""")
            nameMatch = nameRegex.find(text)
            if (nameMatch != null) {
                isClass = true
            } else {
                return null
            }
        }

        val className = nameMatch.groupValues[1]

        // Extract content inside the first { and last }
        val startIndex = text.indexOf('{')
        val endIndex = text.lastIndexOf('}')
        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            return null
        }
        val content = text.substring(startIndex + 1, endIndex)

        val fields = mutableListOf<Field>()
        // Split by newlines or semicolons
        val lines = content.split('\n', ';')
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("/*")) continue

            // Handle potential existing decorators (e.g. @Trace name: type)
            // We just want to extract the name and type
            val cleanLine = trimmed.replace(Regex("""@\w+\s+"""), "")

            // Match:
            // 1. name?: type
            // 2. name: type
            // 3. name: type = value
            // 4. private/public/protected name... (modifiers)
            
            // Regex explanation:
            // ^(?:modifiers\s+)?           -> Optional modifiers (private, public, readonly, etc)
            // ([a-zA-Z0-9_]+)              -> Group 1: Name
            // (\??)                        -> Group 2: Optional ?
            // \s*:\s*                      -> Separator
            // ([^=;]+)                     -> Group 3: Type (greedy until = or end)
            // (?:\s*=.*)?$                 -> Optional initializer
            
            val fieldRegex = Regex("""^(?:(?:private|public|protected|readonly)\s+)*([a-zA-Z0-9_]+)(\??)\s*:\s*([^=;]+)(?:\s*=.*)?$""")
            val match = fieldRegex.find(cleanLine)
            
            if (match != null) {
                val name = match.groupValues[1]
                val isOptional = match.groupValues[2] == "?"
                var type = match.groupValues[3].trim()
                
                // Clean up type string
                if (type.endsWith(";")) type = type.dropLast(1)
                
                fields.add(Field(name, type, isOptional))
            }
        }

        return ParsedInterface(className, fields, isClass)
    }
}
