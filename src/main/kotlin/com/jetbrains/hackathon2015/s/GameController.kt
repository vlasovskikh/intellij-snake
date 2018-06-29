package com.jetbrains.hackathon2015.s

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.*
import javax.swing.Timer

/**
 * @author vlan
 */
class GameController(private val editor: Editor) : JPanel() {
  companion object {
    const val REPAINT_TIMEOUT: Int = 280
    private const val HIGH_SPEED_TIMEOUT = 50
    private const val MAX_SAVED_DIRECTION_KEYS = 2
    private const val DOUBLE_SPEED_AT_LENGTH = 40
  }

  private var repaintTimeout = REPAINT_TIMEOUT
  private var glass: JComponent? = null
  private var oldGlassLayoutManager: LayoutManager? = null
  private var oldGlassOpaque = false
  private var active = false
  private var pause = false
  private var debug = false
  private var alive = false
  private val timer = Timer(repaintTimeout) {
    moveSnakeAndRepaint()
  }
  private val chosenDirections: Deque<GameModel.Direction> = LinkedList()
  private var highSpeed = false

  private var gameModel: GameModel? = null

  init {
    background = Color(0, 0, 0, 0)

    addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        val direction = when (e.keyCode) {
          KeyEvent.VK_DOWN -> GameModel.Direction.DOWN
          KeyEvent.VK_LEFT -> GameModel.Direction.LEFT
          KeyEvent.VK_RIGHT -> GameModel.Direction.RIGHT
          KeyEvent.VK_UP -> GameModel.Direction.UP
          else -> null
        }

        if (direction != null && alive) {
          chosenDirections.addFirst(direction)
          while (chosenDirections.size > MAX_SAVED_DIRECTION_KEYS) {
            chosenDirections.pollLast()
          }
        }

        when (e.keyCode) {
          KeyEvent.VK_ESCAPE -> {
            if (active) {
              deactivate()
            }
          }
          KeyEvent.VK_D -> {
            debug = !debug
          }
          KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> {
            if (alive) {
              pause = !pause
            } else {
              start()
            }
          }
          KeyEvent.VK_A -> {
            highSpeed = true
          }
          KeyEvent.VK_Z -> {
            highSpeed = false
          }
        }
      }
    })

    addMouseWheelListener { }
  }

  override fun repaint() {
    super.repaint()
    requestFocus()
  }

  override fun paintComponent(g: Graphics) {
    glass?.repaint()
    super.paintComponent(g)

    val g2d = g as Graphics2D
    val editorColorsScheme = EditorColorsManager.getInstance().globalScheme
    val gutterWidth = (editor as? EditorEx)?.gutterComponentEx?.width ?: 0
    val columnWidth = editor.columnWidth
    val lineHeight = editor.lineHeight
    val (numLines, numColumns) = editor.screenSize
    val topLeftPosition = editor.topLeftVisiblePosition
    val topLeftPoint = editor.visualPositionToXY(topLeftPosition)
    val leftColOffset = topLeftPoint.x - editor.scrollingModel.horizontalScrollOffset
    val topLineOffset = topLeftPoint.y - editor.scrollingModel.verticalScrollOffset
    val (topLine, leftColumn) = topLeftPosition

    for (row in topLine until topLine + numLines) {
      for (col in leftColumn until leftColumn + numColumns) {
        val pos = VisualPosition(row, col)
        val cellX = (col - leftColumn) * columnWidth + gutterWidth + leftColOffset
        val cellY = (row - topLine) * lineHeight + topLineOffset
        if (gameModel?.getCellType(pos) == GameModel.CellType.SNAKE) {
          g2d.color = editorColorsScheme.defaultForeground
          g2d.fillRect(cellX, cellY, columnWidth, lineHeight)
        }
        else {
          val color = when (gameModel?.getCellType(pos)) {
            GameModel.CellType.EMPTY -> editorColorsScheme.defaultBackground
            GameModel.CellType.FOOD -> Color(0, 0, 0xFF, 0x18)
            GameModel.CellType.WALL -> Color(0xFF, 0, 0, 0)
            else -> null
          }
          color?.let {
            g2d.color = it
            g2d.fillRect(cellX, cellY, columnWidth, lineHeight)
            if (debug) {
              g2d.color = Color(it.red, it.green, it.blue, 0xFF)
              g2d.drawRect(cellX, cellY, columnWidth, lineHeight)
            }
          }
        }
      }
    }

    if (!alive) {
      drawMessage("GAME OVER! SCORE: ${gameModel?.snakeLength ?: 0}", g2d)
    }
    else if (pause) {
      drawMessage("PAUSE", g2d)
    }
  }

  private fun drawMessage(msg: String, g2d: Graphics2D) {
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val editorColorsScheme = EditorColorsManager.getInstance().globalScheme
    val font = Font(editorColorsScheme.editorFontName, Font.PLAIN, editorColorsScheme.editorFontSize * 5)
    g2d.font = font
    g2d.color = editorColorsScheme.defaultForeground
    val bounds = g2d.getFontMetrics(font).getStringBounds(msg, g2d)
    g2d.drawString(msg,
                   (width - bounds.width.toInt()) / 2,
                   (height - bounds.height.toInt()) / 2 - bounds.y.toInt())
  }

  fun activate() {
    active = true
    val parent = editor.contentComponent
    val glassPane = SwingUtilities.getRootPane(parent)?.glassPane as? JComponent
    glass = glassPane
    if (glassPane != null) {
      with (glassPane) {
        oldGlassLayoutManager = layout
        oldGlassOpaque = isOpaque
        layout = null
        isOpaque = false
        isVisible = true
        add(this@GameController)
        addComponentListener(object: ComponentAdapter() {
          override fun componentResized(e: ComponentEvent) {
            deactivate()
          }
        })
      }
      SwingUtilities.getAncestorOfClass(JScrollPane::class.java, parent)?.let {
        val bounds = it.bounds
        bounds.location = SwingUtilities.convertPoint(it.parent, bounds.location, glassPane)
        setBounds(bounds)
      }
    }
    start()
  }

  fun start() {
    alive = true
    gameModel = GameModel(editor)
    timer.start()
    repaint()
  }

  private fun stop() {
    alive = false
    highSpeed = false
    chosenDirections.clear()
    timer.stop()
  }

  fun deactivate() {
    timer.stop()
    active = false
    glass?.let {
      with (it) {
        layout = oldGlassLayoutManager
        isOpaque = oldGlassOpaque
        isVisible = false
        remove(this@GameController)
      }
    }
    editor.contentComponent.requestFocus()
  }

  private fun moveSnakeAndRepaint() {
    if (alive) {
      if (!pause) {
        val direction = chosenDirections.pollLast()
        direction?.let {
          gameModel?.headDirection = it
        }
        if (gameModel?.move() != true) {
          stop()
        }
        val timeIsotropy = when (gameModel?.headDirection ?: GameModel.Direction.RIGHT) {
          GameModel.Direction.RIGHT, GameModel.Direction.LEFT ->
            1.0
          GameModel.Direction.DOWN, GameModel.Direction.UP ->
            editor.columnWidth.toDouble() / editor.lineHeight
        }
        val length = gameModel?.snakeLength ?: 0
        val speedup = (1.0 + length.toDouble() / DOUBLE_SPEED_AT_LENGTH.toDouble())
        val timeout = (REPAINT_TIMEOUT / speedup).toInt()
        val isotropicTimeout = if (highSpeed && timeout > HIGH_SPEED_TIMEOUT) HIGH_SPEED_TIMEOUT else timeout
        repaintTimeout = (isotropicTimeout / timeIsotropy).toInt()
        timer.stop()
        timer.initialDelay = repaintTimeout
        timer.start()
      }
      repaint()
    }
  }
}
