package com.jetbrains.hackathon2015.s

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.impl.EditorImpl
import java.awt.Dimension
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Point

val Editor.screenSize: Pair<Int, Int>
  get() = Pair(visibleLines, visibleColumns)

internal fun Editor.visualPositionToOffset(position: VisualPosition): Int =
    logicalPositionToOffset(visualToLogicalPosition(position))

internal val Editor.topLeftVisiblePosition: VisualPosition
  get() = VisualPosition(topVisualLine, leftVisualColumn)

internal val Editor.topVisualLine: Int
  get() {
    val height = lineHeight
    return when {
      height != 0 -> (scrollingModel.verticalScrollOffset + height - 1) / height
      else -> 0
    }
  }

internal val Editor.leftVisualColumn: Int
  get() {
    val width = columnWidth
    return when {
      width != 0 -> (scrollingModel.horizontalScrollOffset + width - 1) / width
      else -> 0
    }
  }

internal val Editor.columnWidth: Int
  get() = fontMetrics.charWidth('A')

val Editor.fontMetrics: FontMetrics
  get() =
    (this as EditorImpl).getFontMetrics(Font.PLAIN)

internal val Editor.visibleColumns: Int
  get() = xyToVisualPosition(Point(scrollingModel.visibleArea.width, 0)).column

internal val Editor.visibleLines: Int
  get() {
    val lineHeight = lineHeight
    val visibleArea = scrollingModel.visibleArea
    val height = visibleArea.y + visibleArea.height - topVisualLine * lineHeight
    return height / lineHeight
  }

internal fun Editor.activateSnake() {
  GameController(this).activate()
}

operator fun Dimension.component1(): Int = width
operator fun Dimension.component2(): Int = height
operator fun VisualPosition.component1(): Int = line
operator fun VisualPosition.component2(): Int = column
