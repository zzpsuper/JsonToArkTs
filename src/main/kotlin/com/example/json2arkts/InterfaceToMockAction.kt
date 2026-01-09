package com.example.json2arkts

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages

class InterfaceToMockAction : AnAction() {
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

        val mockCode = generateMock(parsedInterface)

        WriteCommandAction.runWriteCommandAction(project) {
            // Insert after selection
            val endOffset = selectionModel.selectionEnd
            document.insertString(endOffset, "\n\n$mockCode")
            selectionModel.removeSelection()
        }
    }

    private fun generateMock(parsed: ParsedInterface): String {
        val sb = StringBuilder()
        // Convert Interface name to camelCase for variable name if possible, or just mockName
        val varName = "mock${parsed.name}"
        
        sb.append("export const $varName: ${parsed.name} = {\n")
        
        val fieldsSize = parsed.fields.size
        parsed.fields.forEachIndexed { index, field ->
            val defaultValue = getMockValue(field.type, field.name)
            sb.append("    ${field.name}: $defaultValue")
            if (index < fieldsSize - 1) {
                sb.append(",\n")
            } else {
                sb.append("\n")
            }
        }
        
        sb.append("};")
        return sb.toString()
    }

    private fun getMockValue(type: String, fieldName: String): String {
        return when {
            type.contains("string", ignoreCase = true) -> "'$fieldName'"
            type.contains("number", ignoreCase = true) -> "0"
            type.contains("boolean", ignoreCase = true) -> "true"
            type.contains("[]") || type.contains("Array<") -> "[]"
            else -> "null"
        }
    }
}
