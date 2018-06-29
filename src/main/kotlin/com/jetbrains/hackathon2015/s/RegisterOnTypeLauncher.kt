package com.jetbrains.hackathon2015.s

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryAdapter
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.TextRange
import java.util.regex.Pattern

/**
 * @author Mikhail Golubev
 */
class RegisterOnTypeLauncher : StartupActivity {
  companion object {
    val SNAKE_WORD_PATTERN = """\bsnake\b""".toPattern(Pattern.CASE_INSENSITIVE)
  }
  private val seenDocuments = hashSetOf<Document>()

  override fun runActivity(project: Project) {
    seenDocuments.clear()

    val application = ApplicationManager.getApplication()
    val editorFactory = EditorFactory.getInstance()
    editorFactory.allEditors.forEach {
      addDocumentListener(it)
    }

    editorFactory.addEditorFactoryListener(object : EditorFactoryAdapter() {
      override fun editorCreated(event: EditorFactoryEvent) {
        addDocumentListener(event.editor)
      }
    }, application)
  }

  private fun addDocumentListener(editor: Editor) {
    val document = editor.document
    val project = editor.project
    if (document in seenDocuments || project == null) {
      return
    }
    seenDocuments.add(document)
    document.addDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        val caretOffset = editor.caretModel.offset
        val lineNumber = document.getLineNumber(caretOffset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineContent = document.getText(TextRange(lineStart, lineEnd))
        val matcher = SNAKE_WORD_PATTERN.matcher(lineContent)
        var start = 0
        while (matcher.find(start)) {
          if ((caretOffset - lineStart) in matcher.start() until matcher.end()) {
            FileEditorManager.getInstance(project).selectedTextEditor?.activateSnake()
          }
          start = matcher.end()
        }
      }
    }, ApplicationManager.getApplication())
  }
}