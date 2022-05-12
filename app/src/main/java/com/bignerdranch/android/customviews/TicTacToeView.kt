package com.bignerdranch.android.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates


typealias OnCellActionListener = (row:Int, column:Int, field:TicTacToeField) -> Unit

class TicTacToeView(
    context:Context,
    attributesSet:AttributeSet?,
    defStyleAttr:Int,
    defStyleRes:Int
) : View(context,attributesSet,defStyleAttr,defStyleRes) {

     //var ticTacToeField:TicTacToeField = TicTacToeField(3,3) (если не нравится нул,то вот)
     var ticTacToeField:TicTacToeField? = null
        set(value){
            // удаляем лисенер из старого поля
            field?.listeners?.remove (listener)
            field = value
            // добавляем лисенер в новое
            value?.listeners?.add(listener)

            updateViewSizes()
            // Метод необходимый для изменения размера компонентна
            requestLayout()
            // Спец метод который перересует компонент
            invalidate()
        }

    var actionListener:OnCellActionListener? = null

    private var player1Color by Delegates.notNull<Int>()
    private var player2Color by Delegates.notNull<Int>()
    private var gridColor by Delegates.notNull<Int>()

    private val fieldRect = RectF(0f,0f,0f,0f)
    private var cellSize : Float = 0f
    private var cellPadding:Float = 0f

    private val cellRect = RectF()

    private lateinit var player1Paint:Paint
    private lateinit var player2Paint:Paint
    private lateinit var gridPaint:Paint

    //----------------------------------
    // Реализуем все конструкторы для вью,каждый из них ссылается на следующий и заполняет

    // Используется если компонент нужно создать с использованием стандартного стиля
    // (здесь стиль по умолчанию,если не нашли(не указали) стандартный стиль и не нашли атрибута в разметке)
    constructor(context: Context,attributesSet: AttributeSet?,defStyleAttr: Int):this(context,attributesSet,defStyleAttr,R.style.DefaultTicTacToeFieldStyle)

    // Создаёт вью (если вы создали компонент в xml файле),сюда придут атрибуты в xml файле
    // (здесь стандартный стиль,его мы указали в атрибутах,если не указан в разметке и в стиле,то будет этот)
    constructor(context: Context,attributesSet: AttributeSet?):this(context,attributesSet,R.attr.ticTacToeFieldStyle)

    // Создаёт вью из кода,без xml файла
    constructor(context: Context):this(context,null)

    //----------------------------------
    init {
        if(attributesSet != null){
            initAttributes(attributesSet,defStyleAttr,defStyleRes)
        } else{
            initDefaultColors()
        }
        initPaints()
        if(isInEditMode){
            ticTacToeField = TicTacToeField(8,6)
            ticTacToeField?.setCell(4,2,Cell.PLAYER_1)
            ticTacToeField?.setCell(4,3,Cell.PLAYER_2)
        }
    }

    private fun initPaints() {
        player1Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        player1Paint.color = player1Color
        player1Paint.style = Paint.Style.STROKE
        player1Paint.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,3f,resources.displayMetrics)

        player2Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        player2Paint.color = player2Color
        player2Paint.style = Paint.Style.STROKE
        player2Paint.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,3f,resources.displayMetrics)

        gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        gridPaint.color = gridColor
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,1f,resources.displayMetrics)
    }

    // Этот метод вызывается когда наша вьюшка уже присоеденена

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ticTacToeField?.listeners?.add(listener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ticTacToeField?.listeners?.remove(listener)
    }

    //если нигде ничего не указано,то будет стандартные значения  PLAYER1_DEFAULT_COLOR и тд
    private fun initAttributes(attributesSet: AttributeSet?,defStyleAttr: Int,defStyleRes: Int){
        val typedArray = context.obtainStyledAttributes(attributesSet,R.styleable.TicTacToeView,defStyleAttr,defStyleRes)
        player1Color = typedArray.getColor(R.styleable.TicTacToeView_player1Color, PLAYER1_DEFAULT_COLOR)
        player2Color = typedArray.getColor(R.styleable.TicTacToeView_player2Color, PLAYER2_DEFAULT_COLOR)
        gridColor = typedArray.getColor(R.styleable.TicTacToeView_gridColor, GRID_DEFAULT_COLOR)

        typedArray.recycle()
    }

    private fun initDefaultColors(){
        player1Color = PLAYER1_DEFAULT_COLOR
        player2Color = PLAYER2_DEFAULT_COLOR
        gridColor = GRID_DEFAULT_COLOR
    }

    // Методы работающие с размером компонента onMeasure(вызывает при измерении размера вью) onSizeChanged(вызывается когда размеры уже определены)
    // Когда нам назначили ширину и высоту
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
         updateViewSizes()
    }

    // Договариваемся с компановщиком о размере
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val minHeight = suggestedMinimumHeight + paddingTop + paddingBottom

        val desiredCellSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            DESIRED_CELL_SIZE,resources.displayMetrics).toInt()
        val rows = ticTacToeField?.rows?:0
        val columns = ticTacToeField?.columns?:0

        val desiredWidth = max(minWidth, columns * desiredCellSizeInPixels + paddingLeft + paddingRight)
        val desiredHeight = max(minHeight,rows * desiredCellSizeInPixels + paddingTop + paddingBottom)

        setMeasuredDimension(
            resolveSize(desiredWidth,widthMeasureSpec),
            resolveSize(desiredHeight,heightMeasureSpec)
        )

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val field = this.ticTacToeField ?: return false
        when(event.action){
            MotionEvent.ACTION_DOWN -> {
                return true
            }
            MotionEvent.ACTION_UP -> {
                val row = getRow(event)
                val column = getColumn(event)
                if(row >= 0 && column >= 0 && row < field.rows && column < field.columns){
                    actionListener?.invoke(row,column,field)
                    return true
                }
                return false
            }
        }
        return false

    }
    // Этот лисенер мы назначили,внутри себя он вызывает setCell,та в свою очередь запускает все лисенеры которые мы назначили

    // Т.к отсчёт должен идти от безопасной зоны,то небезопасную зону мы должны отнять от координат
    private fun getRow(event: MotionEvent):Int{
        return ((event.y - fieldRect.top) / cellSize).toInt()
    }

    private fun getColumn(event: MotionEvent):Int{
        return ((event.x - fieldRect.left) / cellSize).toInt()
    }

    // Метод для рисования
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if(ticTacToeField == null) return
        if(cellSize == 0f) return
        if(fieldRect.width() <= 0) return
        if(fieldRect.height() <= 0) return

        drawGrid(canvas)
        drawCells(canvas)
    }

    private fun drawGrid(canvas: Canvas){
        val field = this.ticTacToeField ?: return

        val xStart = fieldRect.left
        val xEnd = fieldRect.right
        // Рисуем строки
        for(i in 0..field.rows){
            val y = fieldRect.top + cellSize * i
            canvas.drawLine(xStart,y,xEnd,y,gridPaint)
        }

        val yStart = fieldRect.top
        val yEnd = fieldRect.bottom
        // Рисуем колонки
        for(i in 0..field.columns){
            val x = fieldRect.left + cellSize * i
            canvas.drawLine(x,yStart,x,yEnd,gridPaint)
        }

    }

    private fun drawCells(canvas: Canvas){
        val field = this.ticTacToeField ?:return
        for (row in 0 until field.rows){
            for(column in 0 until field.columns){
                val cell = field.getCell(row, column)
                if (cell == Cell.PLAYER_1){
                    drawPlayer1(canvas,row,column)
                } else if(cell == Cell.PLAYER_2){
                    drawPlayer2(canvas,row,column)
                }
            }
        }
    }

    private fun drawPlayer1(canvas: Canvas,row:Int,column:Int){
        val cellRect = getCellRect(row, column)
        // Нужно указать откуда,сверху слево и куда,а потом указать кисть
        canvas.drawLine(cellRect.left, cellRect.top, cellRect.right ,cellRect.bottom,player1Paint)
        canvas.drawLine(cellRect.right, cellRect.top, cellRect.left ,cellRect.bottom,player1Paint)
    }

    private fun drawPlayer2(canvas: Canvas,row:Int,column:Int){
        val cellRect = getCellRect(row, column)
        canvas.drawCircle(cellRect.centerX(), cellRect.centerY(),cellRect.width() / 2, player2Paint)
    }

    private fun getCellRect(row:Int,column:Int):RectF{
        cellRect.left = fieldRect.left + column * cellSize + cellPadding
        cellRect.top  = fieldRect.top + row * cellSize + cellPadding
        cellRect.right = cellRect.left + cellSize - cellPadding * 2
        cellRect.bottom = cellRect.top + cellSize - cellPadding * 2
        return cellRect
    }

    private fun updateViewSizes(){
        val field = this.ticTacToeField ?: return

        val safeWidth = width - paddingLeft - paddingRight
        val safeHeight = height - paddingTop - paddingBottom

        val cellWidth = safeWidth / field.columns.toFloat()
        val cellHeight = safeHeight / field.rows.toFloat()

        // Ширина и восота может быть разной,но ячейки должны быть одинаковыми,поэтому мы берём минимальное значение из этих 2
        cellSize = min(cellWidth,cellHeight)
        // Падинги будут составлять 20 процентов от клетки
        cellPadding = cellSize * 0.2F

        // Общая шикирная и высота сетки
        val fieldWidth = cellSize * field.columns
        val fieldHeight = cellSize * field.rows

        // Если безопасная ширина и ширина поля совпадают,то там будет 0
        // 0 делить на 2 будет 0 и начало совпадёт с падингом
        // а если не совпадает,тогда оно будет центрированно
        fieldRect.left = paddingLeft + (safeWidth - fieldWidth) / 2

        fieldRect.top = paddingTop + (safeHeight - fieldHeight) / 2

        fieldRect.right = fieldRect.left + fieldWidth

        fieldRect.bottom = fieldRect.top + fieldHeight
    }

    // Перерисовываем вью
    private val listener:OnFieldsChangedListener = {
        invalidate()
    }

    //-----------------------------------------------
    //Сохраняем состояние
    /*
    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()!!
        val savedState = SavedState(superState)
        savedState.savedString = "Text"
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        // и здесь текст ставим туда куда нужно
    }

    // Необходимо создать класс собственного состояния и отнаследовать его от BaseSavedState
    class SavedState:BaseSavedState{
        var savedString:String? = null
        constructor(superState:Parcelable):super(superState)
        constructor(parcel:Parcel):super(parcel){
            savedString = parcel.readString()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeString(savedString)
        }
        // Ниже названия имеят значение
        companion object{
            @JvmField
            val CREATOR:Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState>{
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return Array(size){ null }
                }

            }
        }

    }
    */
    //-----------------------------------------------

    companion object{
        const val PLAYER1_DEFAULT_COLOR = Color.GREEN
        const val PLAYER2_DEFAULT_COLOR = Color.RED
        const val GRID_DEFAULT_COLOR = Color.GRAY

        const val DESIRED_CELL_SIZE = 50f
    }
}
