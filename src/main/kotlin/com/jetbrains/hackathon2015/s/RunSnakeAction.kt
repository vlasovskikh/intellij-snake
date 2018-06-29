package com.jetbrains.hackathon2015.s

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction

/**
 * @author vlan
 */
class RunSnakeAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    FileEditorManager.getInstance(project).selectedTextEditor?.activateSnake()
  }
}
