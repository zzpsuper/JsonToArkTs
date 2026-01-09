package com.example.json2arkts

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class JsonToArktsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR)
        
        val selectedText = editor?.selectionModel?.selectedText
        
        // 始终显示 Dialog，如果有选中文本则预填充
        val dialog = JsonToArktsDialog()
        
        if (!selectedText.isNullOrBlank()) {
             dialog.jsonTextArea.text = selectedText
        }
        
        if (dialog.showAndGet()) {
            val json = dialog.getJsonText()
            val className = dialog.getClassName()
            val useClass = dialog.isUseClass()
            val useObservedV2 = dialog.isObservedV2()
            
            if (json.isNotBlank()) {
                generateAndInsert(project, editor, json, className, useClass, useObservedV2)
            }
        }
    }
    
    private fun generateAndInsert(project: Project, editor: Editor?, json: String, className: String, useClass: Boolean, useObservedV2: Boolean) {
        val generator = Generator()
        val result = generator.generate(json, className, useClass, useObservedV2)
        
        if (result.startsWith("// Error")) {
            Messages.showErrorDialog(project, result, "Generation Failed")
            return
        }
        
        if (editor != null) {
            WriteCommandAction.runWriteCommandAction(project) {
                val document = editor.document
                val selectionModel = editor.selectionModel
                if (selectionModel.hasSelection()) {
                    document.replaceString(selectionModel.selectionStart, selectionModel.selectionEnd, result)
                    selectionModel.removeSelection()
                } else {
                    val offset = editor.caretModel.offset
                    document.insertString(offset, result)
                }
            }
        } else {
             // Copy to clipboard if no editor open
             val selection = StringSelection(result)
             Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
             Messages.showInfoMessage(project, "Generated code copied to clipboard.", "Success")
        }
    }
}
