package com.jetbrains.hackathon2015.s


import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import java.util.*

/**
 * @author Mikhail Golubev
 */
class GameModel(private val editor: Editor) {
  companion object {
    const val INITIAL_SNAKE_LENGTH = 3
    val ANTI_PYTHON_CHARS = setOf('{', '}', ';')
  }

  enum class Direction {
    UP,
    RIGHT,
    DOWN,
    LEFT;

    val opposite: Direction
      get() = when(this) {
        UP -> DOWN
        RIGHT -> LEFT
        DOWN -> UP
        LEFT -> RIGHT
      }
  }

  enum class CellType {
    FOOD,
    WALL,
    EMPTY,
    SNAKE
  }

  var headDirection = Direction.RIGHT
    set(direction) {
      if (direction != field.opposite) {
        field = direction
      }
    }

  val snakeLength: Int
    get() {
      return snakeCells.size
    }

  private val eatenCells: HashSet<VisualPosition> = hashSetOf()
  private val snakeCells: Deque<VisualPosition> = LinkedList()
  private val charCache: HashMap<VisualPosition, Char> = hashMapOf()

  init {
    val tailCell = findBestSnakePosition()
    for (i in 0 until INITIAL_SNAKE_LENGTH) {
      snakeCells.addFirst(VisualPosition(tailCell.line, tailCell.column + i))
    }
  }

  /**
   * Returns visual position of the snake's tail
   */
  private fun findBestSnakePosition(): VisualPosition {
    fun countEmptyColumnsAfter(line: Int, startColumn: Int): Int {
      var count = 0
      for (col in startColumn..visibleColumnsRange.endInclusive) {
        val c = getCharAt(line, col)
        if (c.isWhitespace()) {
          count++
        }
        else {
          break
        }
      }
      return count
    }

    for (line in visibleLinesRange) {
      for (col in visibleColumnsRange) {
        // Range end is inclusive
        if (countEmptyColumnsAfter(line, col) >= INITIAL_SNAKE_LENGTH * 2) {
          return VisualPosition(line, col)
        }
      }
    }
    return editor.topLeftVisiblePosition
  }

  /**
   * @return whether next move is possible
   */
  fun move(): Boolean {
    var (newLine, newCol) = snakeCells.first
    when (headDirection) {
      Direction.DOWN -> newLine += 1
      Direction.RIGHT -> newCol += 1
      Direction.UP -> newLine -= 1
      Direction.LEFT -> newCol -= 1
    }
    if (newCol !in visibleColumnsRange || newLine !in visibleLinesRange) {
      return false
    }
    val newHead = VisualPosition(newLine, newCol)
    if (newHead in snakeCells) {
      return false
    }
    val cellType = getCellType(newHead)
    // Remove the tail *before* the check that snake is going to turn to itself
    if (cellType == CellType.EMPTY) {
      snakeCells.removeLast()
    }
    when (cellType) {
      CellType.WALL -> return false
      CellType.SNAKE -> return false
      CellType.FOOD -> eatenCells.add(newHead)
      CellType.EMPTY -> {}
    }
    snakeCells.addFirst(newHead)
    return true
  }

  fun getCellType(pos: VisualPosition): CellType {
    if (pos in snakeCells) {
      return CellType.SNAKE
    }
    val char = getCharAt(pos)
    return when {
      char.isWhitespace() || pos in eatenCells -> CellType.EMPTY
      char in ANTI_PYTHON_CHARS -> CellType.FOOD
      else -> CellType.WALL
    }
  }

  private fun getCharAt(line: Int, col: Int): Char = getCharAt(VisualPosition(line, col))

  private fun getCharAt(pos: VisualPosition): Char {
    charCache[pos]?.let {
      return it
    }
    val offset = editor.visualPositionToOffset(pos)
    val document = editor.document
    val result = if (offset < document.textLength) document.charsSequence[offset] else ' '
    charCache[pos] = result
    return result
  }

  private val visibleColumnsRange: IntRange
    get() = editor.leftVisualColumn until editor.leftVisualColumn + editor.visibleColumns

  private val visibleLinesRange: IntRange
    get() = editor.topVisualLine until editor.topVisualLine + editor.visibleLines
}
