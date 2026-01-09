package com.example.json2arkts

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.JLabel
import java.awt.BorderLayout
import javax.swing.Box
import javax.swing.BoxLayout

import javax.swing.JCheckBox

class JsonToArktsDialog : DialogWrapper(true) {
    val jsonTextArea = JTextArea(20, 50)
    val classNameField = JTextField("Root", 20)
    val useClassCheckBox = JCheckBox("Generate Class instead of Interface")
    val observedV2CheckBox = JCheckBox("Add @ObservedV2 (with @Trace)")

    init {
        title = "JSON to ArkTS"
        init()
        
        useClassCheckBox.addActionListener {
            observedV2CheckBox.isEnabled = useClassCheckBox.isSelected
        }
        observedV2CheckBox.isEnabled = false // Disabled by default until Class is checked
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))
        
        val topPanel = JPanel()
        topPanel.layout = BoxLayout(topPanel, BoxLayout.Y_AXIS)
        
        val row1 = JPanel(BorderLayout())
        val namePanel = JPanel()
        namePanel.add(JLabel("Root Class Name: "))
        namePanel.add(classNameField)
        row1.add(namePanel, BorderLayout.WEST)
        
        val row2 = JPanel(BorderLayout())
        val checkPanel = JPanel()
        checkPanel.add(useClassCheckBox)
        checkPanel.add(observedV2CheckBox)
        row2.add(checkPanel, BorderLayout.WEST)
        
        topPanel.add(row1)
        topPanel.add(row2)
        
        panel.add(topPanel, BorderLayout.NORTH)
        
        val centerPanel = JPanel(BorderLayout())
        centerPanel.add(JLabel("Paste JSON here:"), BorderLayout.NORTH)
        centerPanel.add(JBScrollPane(jsonTextArea), BorderLayout.CENTER)
        
        panel.add(centerPanel, BorderLayout.CENTER)
        
        return panel
    }
    
    fun getJsonText(): String = jsonTextArea.text
    fun getClassName(): String = classNameField.text
    fun isUseClass(): Boolean = useClassCheckBox.isSelected
    fun isObservedV2(): Boolean = observedV2CheckBox.isSelected
}
