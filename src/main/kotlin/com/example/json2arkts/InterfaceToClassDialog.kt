package com.example.json2arkts

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import javax.swing.*
import java.awt.BorderLayout

class InterfaceToClassDialog(private val parsedInterface: ParsedInterface) : DialogWrapper(true) {
    private val observedV2CheckBox = JCheckBox("Add @ObservedV2 annotation")
    private val fieldCheckboxes = mutableListOf<JCheckBox>()
    private val fieldsPanel = JPanel()
    private val selectAllBox = JCheckBox("Select All Fields for @Trace")

    init {
        title = "Interface to Class Options"
        
        fieldsPanel.layout = BoxLayout(fieldsPanel, BoxLayout.Y_AXIS)
        
        // Add "Select All" helper
        selectAllBox.addActionListener {
            val selected = selectAllBox.isSelected
            fieldCheckboxes.forEach { it.isSelected = selected }
        }
        fieldsPanel.add(selectAllBox)
        fieldsPanel.add(JSeparator())

        parsedInterface.fields.forEach { field ->
            val cb = JCheckBox(field.name)
            cb.isSelected = true // Default to selected
            fieldCheckboxes.add(cb)
            fieldsPanel.add(cb)
        }

        // Enable/Disable fields based on V2 checkbox
        observedV2CheckBox.addActionListener {
            updateState()
        }
        
        // Initial state
        // If it's already a class, we might want to default to true? Or false.
        // Let's keep it false by default to force user choice.
        observedV2CheckBox.isSelected = false
        updateState()

        init()
    }

    private fun updateState() {
        val v2Enabled = observedV2CheckBox.isSelected
        selectAllBox.isEnabled = v2Enabled
        fieldCheckboxes.forEach { it.isEnabled = v2Enabled }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(10, 10))
        panel.add(observedV2CheckBox, BorderLayout.NORTH)
        
        val scrollPane = JBScrollPane(fieldsPanel)
        scrollPane.border = BorderFactory.createTitledBorder("Add @Trace to properties:")
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }

    fun isObservedV2(): Boolean = observedV2CheckBox.isSelected
    
    fun getTracedFields(): Set<String> {
        val result = mutableSetOf<String>()
        if (!isObservedV2()) return result
        
        fieldCheckboxes.forEach { 
            if (it.isSelected) {
                result.add(it.text)
            }
        }
        return result
    }
}
