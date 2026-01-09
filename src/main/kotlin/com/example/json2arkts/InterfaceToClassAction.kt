package com.example.json2arkts

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages

class InterfaceToClassAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText

        if (selectedText.isNullOrBlank()) {
            Messages.showInfoMessage(project, "Please select an Interface definition first.", "Info")
            return
        }

        val parser = InterfaceParser()
        val parsedInterface = parser.parse(selectedText)
        
        if (parsedInterface == null) {
            Messages.showErrorDialog(project, "Could not parse Interface. Ensure valid selection.", "Error")
            return
        }

        val dialog = InterfaceToClassDialog(parsedInterface)
        if (dialog.showAndGet()) {
            val isV2 = dialog.isObservedV2()
            val tracedFields = dialog.getTracedFields()
            
            val finalCode = if (parsedInterface.isClass) {
                injectAnnotations(selectedText, isV2, tracedFields)
            } else {
                generateClass(parsedInterface, isV2, tracedFields)
            }

            WriteCommandAction.runWriteCommandAction(project) {
                document.replaceString(selectionModel.selectionStart, selectionModel.selectionEnd, finalCode)
                selectionModel.removeSelection()
            }
        }
    }

    private fun injectAnnotations(originalCode: String, isV2: Boolean, tracedFields: Set<String>): String {
        // If user didn't check V2, maybe they just want to add Trace? 
        // But usually V2 implies ObservedV2. 
        // If isV2 is false, we technically shouldn't add ObservedV2, but what about Trace?
        // Assuming if !isV2, we do nothing or just remove annotations? 
        // For now, let's assume we only ADD.
        
        var code = originalCode
        
        // 1. Add @ObservedV2 if needed
        if (isV2 && !code.contains("@ObservedV2")) {
            // Find "export class" or "class"
            val classRegex = Regex("""(export\s+)?class\s+\w+""")
            val match = classRegex.find(code)
            if (match != null) {
                val insertIndex = match.range.first
                code = code.substring(0, insertIndex) + "@ObservedV2\n" + code.substring(insertIndex)
            }
        }
        
        // 2. Add @Trace to fields
        // We iterate over the known fields and use regex to replace them in the code
        // This is a bit tricky if there are multiple fields with same name (unlikely in valid TS)
        // We use the field name to find the line.
        
        for (field in tracedFields) {
            // Regex to find the field definition:
            // "name: type" or "name?: type" or "name = val"
            // We want to make sure we don't double add @Trace
            // Pattern: start of line or whitespace -> (no @Trace) -> name -> ...
            
            // We look for the specific field name followed by : or = or ?
            // And ensure it is NOT preceded by @Trace
            
            val fieldPattern = Regex("""(\n\s*)(?!@Trace\s+)(\b${Regex.escape(field)}\b\s*[:=?])""")
            
            // We replace with "\n  @Trace field..."
            // We need to preserve the indentation.
            
            code = code.replace(fieldPattern) { matchResult ->
                val indent = matchResult.groupValues[1]
                val rest = matchResult.groupValues[2]
                "$indent@Trace $rest"
            }
        }
        
        return code
    }

    private fun generateClass(parsed: ParsedInterface, isV2: Boolean, tracedFields: Set<String>): String {
        val sb = StringBuilder()
        
        if (isV2) {
            sb.append("@ObservedV2\n")
        }
        sb.append("export class ${parsed.name} {\n")
        
        for (field in parsed.fields) {
            val defaultValue = getDefaultValue(field.type)
            val optionalStr = if (field.isOptional) "?" else ""
            
            if (isV2 && tracedFields.contains(field.name)) {
                sb.append("    @Trace ")
            } else {
                sb.append("    ")
            }
            
            // ArkTS requires initializers or optional
            // We provide default values for convenience
            sb.append("${field.name}$optionalStr: ${field.type} = $defaultValue;\n")
        }
        
        sb.append("}")
        return sb.toString()
    }

    private fun getDefaultValue(type: String): String {
        return when {
            type.contains("string", ignoreCase = true) -> "''"
            type.contains("number", ignoreCase = true) -> "0"
            type.contains("boolean", ignoreCase = true) -> "false"
            type.contains("[]") || type.contains("Array<") -> "[]"
            else -> "null"
        }
    }
}
